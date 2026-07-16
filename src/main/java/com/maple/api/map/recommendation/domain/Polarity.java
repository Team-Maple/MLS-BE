package com.maple.api.map.recommendation.domain;

import java.util.Arrays;

public enum Polarity {
    POSITIVE("positive", 1),
    NEGATIVE("negative", -1);

    private final String dbValue;
    private final int sign;

    Polarity(String dbValue, int sign) {
        this.dbValue = dbValue;
        this.sign = sign;
    }

    public String dbValue() {
        return dbValue;
    }

    public int sign() {
        return sign;
    }

    public static Polarity from(String value) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.dbValue.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported polarity: " + value));
    }
}
