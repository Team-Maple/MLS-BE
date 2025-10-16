package com.maple.api.item.application.dto;

import com.maple.api.item.domain.RequiredStats;

public record RequiredStatsDto(
        Integer level,
        Integer str,
        Integer dex,
        Integer intelligence,
        Integer luk,
        Integer pop
) {
    public static RequiredStatsDto toDto(RequiredStats requiredStats) {
        if (requiredStats == null) {
            return null;
        }
        
        return new RequiredStatsDto(
                nullIfZero(requiredStats.getLevel()),
                nullIfZero(requiredStats.getStr()),
                nullIfZero(requiredStats.getDex()),
                nullIfZero(requiredStats.getIntelligence()),
                nullIfZero(requiredStats.getLuk()),
                nullIfZero(requiredStats.getPop())
        );
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
