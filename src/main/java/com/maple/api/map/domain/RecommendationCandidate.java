package com.maple.api.map.domain;

import java.util.List;
import java.util.Objects;

public record RecommendationCandidate(long mapId, double score, List<RecommendationReason> reasons) {

    public RecommendationCandidate {
        if (!Double.isFinite(score)) {
            throw new IllegalArgumentException("score must be finite");
        }
        reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons must not be null"));
    }
}
