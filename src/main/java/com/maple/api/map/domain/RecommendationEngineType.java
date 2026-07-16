package com.maple.api.map.domain;

import java.util.Locale;

public enum RecommendationEngineType {
    AURA,
    MYSQL;

    public String metricValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
