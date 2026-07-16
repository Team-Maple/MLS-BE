package com.maple.api.map.recommendation.port;

import java.util.Locale;
import java.util.Optional;

public enum RecommendationEngineType {
    AURA,
    MYSQL;

    public static Optional<RecommendationEngineType> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public String metricValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
