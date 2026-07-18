package com.maple.api.map.application.dto;

import com.maple.api.map.domain.RecommendationReason;

import java.util.List;
import java.util.Objects;

public record MapRecommendationResultDto(
        Integer mapId,
        double score,
        String iconUrl,
        String nameKr,
        Integer bookmarkId,
        List<RecommendationReason> reasons
) {
    public MapRecommendationResultDto {
        if (!Double.isFinite(score)) {
            throw new IllegalArgumentException("score must be finite");
        }
        reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons must not be null"));
    }
}
