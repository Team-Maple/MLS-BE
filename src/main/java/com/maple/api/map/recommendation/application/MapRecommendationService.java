package com.maple.api.map.recommendation.application;

import com.maple.api.common.presentation.exception.ApiException;
import com.maple.api.map.application.dto.MapRecommendationDto;
import com.maple.api.map.application.dto.MapRecommendationV2Dto;
import com.maple.api.map.exception.MapException;
import com.maple.api.map.recommendation.config.RecommendationProperties;
import com.maple.api.map.recommendation.port.RecommendationEngineType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MapRecommendationService {

    static final int DEFAULT_LIMIT = 5;

    private final RecommendationProperties recommendationProperties;
    private final RecommendationQueryExecutor queryExecutor;
    private final MapRecommendationV1Mapper v1Mapper;
    private final MapRecommendationV2Mapper v2Mapper;
    private final RecommendationObservability observability;

    public List<MapRecommendationDto> recommendV1(
            String memberId,
            int level,
            int jobId,
            Integer limit
    ) {
        RecommendationEngineType engine = RecommendationEngineType
                .parse(recommendationProperties.getV1Engine())
                .orElseThrow(() -> ApiException.of(MapException.MAP_RECOMMENDATION_UNAVAILABLE));
        return recommend(memberId, level, jobId, limit, engine, "v1").stream()
                .map(v1Mapper::toDto)
                .toList();
    }

    public List<MapRecommendationV2Dto> recommendV2(
            String memberId,
            int level,
            int jobId,
            Integer limit
    ) {
        if (!recommendationProperties.isV2Enabled()) {
            observability.disabled(RecommendationEngineType.MYSQL, "v2");
            throw ApiException.of(MapException.MAP_RECOMMENDATION_UNAVAILABLE);
        }
        return recommend(memberId, level, jobId, limit, RecommendationEngineType.MYSQL, "v2").stream()
                .map(v2Mapper::toDto)
                .toList();
    }

    private List<EnrichedRecommendationCandidate> recommend(
            String memberId,
            int level,
            int jobId,
            Integer limit,
            RecommendationEngineType engine,
            String apiVersion
    ) {
        int effectiveLimit = limit == null ? DEFAULT_LIMIT : limit;
        long startedAt = System.nanoTime();

        try {
            List<EnrichedRecommendationCandidate> enriched = queryExecutor.execute(
                    memberId,
                    level,
                    jobId,
                    effectiveLimit,
                    engine
            );
            observability.completed(engine, apiVersion, enriched.size(), elapsedSince(startedAt));
            return enriched;
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            observability.unavailable(engine, apiVersion, elapsedSince(startedAt), exception);
            throw ApiException.of(MapException.MAP_RECOMMENDATION_UNAVAILABLE, exception);
        }
    }

    private long elapsedSince(long startedAt) {
        return Math.max(0L, System.nanoTime() - startedAt);
    }
}
