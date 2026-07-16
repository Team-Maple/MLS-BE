package com.maple.api.map.recommendation.domain;

import java.util.Objects;

public record RecommendationReason(String axis, String value) {

    public RecommendationReason {
        Objects.requireNonNull(axis, "axis must not be null");
        Objects.requireNonNull(value, "value must not be null");
    }
}
