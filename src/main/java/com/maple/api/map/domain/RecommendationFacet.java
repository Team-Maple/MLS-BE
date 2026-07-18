package com.maple.api.map.domain;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public enum RecommendationFacet {
    REWARD_XP("reward", "xp", 0),
    REWARD_MESO("reward", "meso", 1),
    REWARD_LOOT("reward", "loot", 2),
    PLAY_STYLE_SOLO("play_style", "solo", 0),
    PLAY_STYLE_PARTY("play_style", "party", 1),
    PLAY_STYLE_PARTY_QUEST("play_style", "party_quest", 2),
    OPERABILITY_FATIGUE("operability", "fatigue", 0),
    OPERABILITY_MOBILITY("operability", "mobility", 1),
    OPERABILITY_BUDGET("operability", "budget", 2);

    private static final Comparator<RecommendationFacet> PRIORITY_ORDER =
            Comparator.comparingInt(RecommendationFacet::priority);

    private final String axis;
    private final String value;
    private final int priority;

    RecommendationFacet(String axis, String value, int priority) {
        this.axis = axis;
        this.value = value;
        this.priority = priority;
    }

    public String axis() {
        return axis;
    }

    public String value() {
        return value;
    }

    public int priority() {
        return priority;
    }

    public static RecommendationFacet from(String axis, String value) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.axis.equalsIgnoreCase(axis)
                        && candidate.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported facet: " + axis + ':' + value));
    }

    public static List<RecommendationFacet> forAxis(String axis) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.axis.equalsIgnoreCase(axis))
                .sorted(PRIORITY_ORDER)
                .toList();
    }
}
