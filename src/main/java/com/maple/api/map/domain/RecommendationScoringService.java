package com.maple.api.map.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure scoring policy for repository evidence. Inputs must already be restricted to APPROVED
 * evidence for the requested job and its ancestors.
 */
public final class RecommendationScoringService {

    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 20;
    private static final BigDecimal PATCH_DECAY = new BigDecimal("0.05");
    private static final BigDecimal MIN_FRESHNESS = new BigDecimal("0.1");
    private static final List<String> REASON_AXIS_ORDER = List.of("reward", "play_style", "operability");

    private static final Comparator<CandidateAggregate> CANDIDATE_ORDER =
            Comparator.comparing(CandidateAggregate::netScore, Comparator.reverseOrder())
                    .thenComparing(CandidateAggregate::freshnessSum, Comparator.reverseOrder())
                    .thenComparing(CandidateAggregate::representativePublishedAt, Comparator.reverseOrder())
                    .thenComparingLong(CandidateAggregate::mapId);

    public List<RecommendationCandidate> score(
            int requestedLevel,
            List<RecommendationEvidence> evidence,
            int limit
    ) {
        validateLimit(limit);
        Objects.requireNonNull(evidence, "evidence must not be null");

        Map<EvidenceKey, RecommendationEvidence> deduplicated = deduplicate(evidence);
        Map<Long, CandidateAggregate> candidatesByMap = new HashMap<>();

        for (RecommendationEvidence row : deduplicated.values()) {
            if (!matchesLevel(requestedLevel, row.levelMin(), row.levelMax())) {
                continue;
            }

            BigDecimal contribution = freshnessWeight(row.patchCount())
                    .multiply(BigDecimal.valueOf(row.polarity().sign()));
            CandidateAggregate aggregate = candidatesByMap.computeIfAbsent(
                    row.mapId(),
                    CandidateAggregate::new
            );
            aggregate.add(row, contribution);
        }

        return candidatesByMap.values().stream()
                .filter(CandidateAggregate::hasPositiveEvidence)
                .filter(candidate -> candidate.netScore().signum() > 0)
                .sorted(CANDIDATE_ORDER)
                .limit(limit)
                .map(CandidateAggregate::toCandidate)
                .toList();
    }

    private Map<EvidenceKey, RecommendationEvidence> deduplicate(List<RecommendationEvidence> evidence) {
        Map<EvidenceKey, RecommendationEvidence> deduplicated = new LinkedHashMap<>();
        for (RecommendationEvidence row : evidence) {
            Objects.requireNonNull(row, "evidence row must not be null");
            EvidenceKey key = new EvidenceKey(row.extractedClaimId(), row.mapId());
            deduplicated.merge(
                    key,
                    row,
                    (current, candidate) -> candidate.reviewedClaimId() < current.reviewedClaimId()
                            ? candidate
                            : current
            );
        }
        return deduplicated;
    }

    private boolean matchesLevel(int requestedLevel, Integer levelMin, Integer levelMax) {
        if (levelMin == null && levelMax == null) {
            return false;
        }

        long effectiveMin = levelMin != null ? levelMin : (long) levelMax - 10L;
        long effectiveMax = levelMax != null ? levelMax : (long) levelMin + 10L;
        return requestedLevel >= effectiveMin && requestedLevel <= effectiveMax;
    }

    private BigDecimal freshnessWeight(int patchCount) {
        BigDecimal decayed = BigDecimal.ONE.subtract(PATCH_DECAY.multiply(BigDecimal.valueOf(patchCount)));
        return decayed.max(MIN_FRESHNESS).setScale(3, RoundingMode.HALF_UP);
    }

    private void validateLimit(int limit) {
        if (limit < MIN_LIMIT || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and 20");
        }
    }

    private record EvidenceKey(long extractedClaimId, long mapId) {
    }

    private static final class CandidateAggregate {
        private final long mapId;
        private final Map<RecommendationFacet, BigDecimal> facetWeights =
                new EnumMap<>(RecommendationFacet.class);
        private BigDecimal netScore = BigDecimal.ZERO;
        private BigDecimal freshnessSum = BigDecimal.ZERO;
        private boolean hasPositiveEvidence;
        private BigDecimal representativeContribution;
        private LocalDateTime representativePublishedAt;

        private CandidateAggregate(long mapId) {
            this.mapId = mapId;
        }

        private void add(RecommendationEvidence evidence, BigDecimal contribution) {
            netScore = netScore.add(contribution);
            freshnessSum = freshnessSum.add(contribution.abs());
            if (evidence.polarity() == Polarity.POSITIVE) {
                hasPositiveEvidence = true;
            }

            if (isBetterRepresentative(contribution, evidence.publishedAt())) {
                representativeContribution = contribution;
                representativePublishedAt = evidence.publishedAt();
            }

            for (RecommendationFacet facet : evidence.reasonFacets()) {
                facetWeights.merge(facet, contribution, BigDecimal::add);
            }
        }

        private boolean isBetterRepresentative(BigDecimal contribution, LocalDateTime publishedAt) {
            if (representativeContribution == null) {
                return true;
            }
            int contributionOrder = contribution.compareTo(representativeContribution);
            return contributionOrder > 0
                    || contributionOrder == 0 && publishedAt.isAfter(representativePublishedAt);
        }

        private boolean hasPositiveEvidence() {
            return hasPositiveEvidence;
        }

        private long mapId() {
            return mapId;
        }

        private BigDecimal netScore() {
            return netScore;
        }

        private BigDecimal freshnessSum() {
            return freshnessSum;
        }

        private LocalDateTime representativePublishedAt() {
            return representativePublishedAt;
        }

        private RecommendationCandidate toCandidate() {
            double score = netScore.doubleValue();
            if (!Double.isFinite(score)) {
                throw new IllegalStateException("Recommendation score is not finite for map " + mapId);
            }
            return new RecommendationCandidate(mapId, score, reasons());
        }

        private List<RecommendationReason> reasons() {
            return REASON_AXIS_ORDER.stream()
                    .map(this::bestFacetForAxis)
                    .filter(Objects::nonNull)
                    .map(facet -> new RecommendationReason(facet.axis(), facet.value()))
                    .toList();
        }

        private RecommendationFacet bestFacetForAxis(String axis) {
            RecommendationFacet bestFacet = null;
            BigDecimal bestWeight = null;
            for (RecommendationFacet candidate : RecommendationFacet.forAxis(axis)) {
                BigDecimal weight = facetWeights.getOrDefault(candidate, BigDecimal.ZERO);
                if (bestWeight == null || weight.compareTo(bestWeight) > 0) {
                    bestFacet = candidate;
                    bestWeight = weight;
                }
            }
            return bestWeight != null && bestWeight.signum() > 0 ? bestFacet : null;
        }
    }
}
