package com.maple.api.map.recommendation.adapter;

import com.maple.api.map.recommendation.domain.Polarity;
import com.maple.api.map.recommendation.domain.RecommendationCandidate;
import com.maple.api.map.recommendation.domain.RecommendationEvidence;
import com.maple.api.map.recommendation.domain.RecommendationFacet;
import com.maple.api.map.recommendation.domain.RecommendationScoringService;
import com.maple.api.map.recommendation.port.RecommendationEnginePort;
import com.maple.api.map.recommendation.port.RecommendationEngineType;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class MySqlRecommendationEvidenceAdapter implements RecommendationEnginePort {

    static final String SCORING_QUERY = """
            WITH RECURSIVE job_lineage (job_id, parent_job_id, lineage_path) AS (
                SELECT j.job_id,
                       j.parent_job_id,
                       CAST(CONCAT(',', j.job_id, ',') AS CHAR(2048)) AS lineage_path
                  FROM jobs j
                 WHERE j.job_id = :jobId

                UNION ALL

                SELECT parent.job_id,
                       parent.parent_job_id,
                       CAST(CONCAT(lineage.lineage_path, parent.job_id, ',') AS CHAR(2048))
                  FROM jobs parent
                  JOIN job_lineage lineage ON parent.job_id = lineage.parent_job_id
                 WHERE LOCATE(CONCAT(',', parent.job_id, ','), lineage.lineage_path) = 0
            ),
            ranked_claims AS (
                SELECT rc.reviewed_claim_id,
                       rc.extracted_claim_id,
                       rc.final_map_id,
                       rc.final_job_id,
                       rc.final_level_min,
                       rc.final_level_max,
                       rc.final_polarity,
                       ec.source_published_at,
                       ROW_NUMBER() OVER (
                           PARTITION BY rc.extracted_claim_id, rc.final_map_id
                           ORDER BY rc.reviewed_claim_id
                       ) AS evidence_rank
                  FROM recommendation_reviewed_claims rc
                  JOIN recommendation_extracted_claims ec
                    ON ec.extracted_claim_id = rc.extracted_claim_id
                  JOIN job_lineage lineage
                    ON lineage.job_id = rc.final_job_id
                  JOIN maps canonical_map
                    ON canonical_map.map_id = rc.final_map_id
                 WHERE rc.review_status = 'APPROVED'
                   AND rc.final_map_id IS NOT NULL
                   AND rc.final_job_id IS NOT NULL
                   AND rc.final_polarity IS NOT NULL
            ),
            deduplicated_claims AS (
                SELECT reviewed_claim_id,
                       extracted_claim_id,
                       final_map_id,
                       final_job_id,
                       final_level_min,
                       final_level_max,
                       final_polarity,
                       source_published_at
                  FROM ranked_claims
                 WHERE evidence_rank = 1
            ),
            patch_counts AS (
                SELECT claim.reviewed_claim_id,
                       COUNT(patch.id) AS patch_count
                  FROM deduplicated_claims claim
                  LEFT JOIN alrim patch
                    ON patch.type = 'PATCH_NOTE'
                   AND patch.date > claim.source_published_at
                 GROUP BY claim.reviewed_claim_id
            )
            SELECT claim.reviewed_claim_id,
                   claim.extracted_claim_id,
                   claim.final_map_id,
                   claim.final_job_id,
                   claim.final_level_min,
                   claim.final_level_max,
                   claim.final_polarity,
                   claim.source_published_at,
                   patch_count.patch_count,
                   reason.reason_axis,
                   reason.reason_value
              FROM deduplicated_claims claim
              JOIN patch_counts patch_count
                ON patch_count.reviewed_claim_id = claim.reviewed_claim_id
              LEFT JOIN recommendation_reviewed_claim_reasons reason
                ON reason.reviewed_claim_id = claim.reviewed_claim_id
             ORDER BY claim.reviewed_claim_id,
                      reason.display_order,
                      reason.reviewed_claim_reason_id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RecommendationScoringService scoringService;

    public MySqlRecommendationEvidenceAdapter(
            @Qualifier("recommendationJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate,
            RecommendationScoringService scoringService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.scoringService = scoringService;
    }

    @Override
    public RecommendationEngineType engineType() {
        return RecommendationEngineType.MYSQL;
    }

    @Override
    public List<RecommendationCandidate> recommend(int level, int jobId, int limit) {
        List<RecommendationEvidence> evidence = loadEvidence(jobId);
        return scoringService.score(level, evidence, limit);
    }

    List<RecommendationEvidence> loadEvidence(int jobId) throws DataAccessException {
        return jdbcTemplate.query(
                SCORING_QUERY,
                Map.of("jobId", jobId),
                (ResultSetExtractor<List<RecommendationEvidence>>) this::extractEvidence
        );
    }

    private List<RecommendationEvidence> extractEvidence(ResultSet resultSet) throws SQLException {
        Map<Long, EvidenceAccumulator> byReviewedClaim = new LinkedHashMap<>();
        while (resultSet.next()) {
            long reviewedClaimId = resultSet.getLong("reviewed_claim_id");
            EvidenceAccumulator accumulator = byReviewedClaim.computeIfAbsent(
                    reviewedClaimId,
                    ignored -> EvidenceAccumulator.from(resultSet)
            );
            String reasonAxis = resultSet.getString("reason_axis");
            String reasonValue = resultSet.getString("reason_value");
            if (reasonAxis != null && reasonValue != null) {
                accumulator.addReason(RecommendationFacet.from(reasonAxis, reasonValue));
            }
        }
        return byReviewedClaim.values().stream()
                .map(EvidenceAccumulator::toEvidence)
                .toList();
    }

    private static Integer nullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private static final class EvidenceAccumulator {
        private final long extractedClaimId;
        private final long reviewedClaimId;
        private final long mapId;
        private final long jobId;
        private final Integer levelMin;
        private final Integer levelMax;
        private final Polarity polarity;
        private final LocalDateTime publishedAt;
        private final int patchCount;
        private final List<RecommendationFacet> reasons = new ArrayList<>();

        private EvidenceAccumulator(
                long extractedClaimId,
                long reviewedClaimId,
                long mapId,
                long jobId,
                Integer levelMin,
                Integer levelMax,
                Polarity polarity,
                LocalDateTime publishedAt,
                int patchCount
        ) {
            this.extractedClaimId = extractedClaimId;
            this.reviewedClaimId = reviewedClaimId;
            this.mapId = mapId;
            this.jobId = jobId;
            this.levelMin = levelMin;
            this.levelMax = levelMax;
            this.polarity = polarity;
            this.publishedAt = publishedAt;
            this.patchCount = patchCount;
        }

        private static EvidenceAccumulator from(ResultSet resultSet) {
            try {
                return new EvidenceAccumulator(
                        resultSet.getLong("extracted_claim_id"),
                        resultSet.getLong("reviewed_claim_id"),
                        resultSet.getLong("final_map_id"),
                        resultSet.getLong("final_job_id"),
                        nullableInteger(resultSet, "final_level_min"),
                        nullableInteger(resultSet, "final_level_max"),
                        Polarity.from(resultSet.getString("final_polarity")),
                        resultSet.getTimestamp("source_published_at").toLocalDateTime(),
                        Math.toIntExact(resultSet.getLong("patch_count"))
                );
            } catch (SQLException exception) {
                throw new EvidenceMappingException(exception);
            }
        }

        private void addReason(RecommendationFacet reason) {
            reasons.add(reason);
        }

        private RecommendationEvidence toEvidence() {
            return new RecommendationEvidence(
                    extractedClaimId,
                    reviewedClaimId,
                    mapId,
                    jobId,
                    levelMin,
                    levelMax,
                    polarity,
                    publishedAt,
                    patchCount,
                    reasons
            );
        }
    }

    private static final class EvidenceMappingException extends RuntimeException {
        private EvidenceMappingException(SQLException cause) {
            super(cause);
        }
    }
}
