package com.maple.api.map.recommendation.application;

import com.maple.api.map.recommendation.domain.RecommendationReason;

import java.util.List;
import java.util.Objects;

public record EnrichedRecommendationCandidate(
        Integer mapId,
        double score,
        String iconUrl,
        String nameKr,
        Integer bookmarkId,
        List<RecommendationReason> reasons
) {
    public EnrichedRecommendationCandidate {
        if (!Double.isFinite(score)) {
            throw new IllegalArgumentException("score must be finite");
        }
        reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons must not be null"));
    }
}
