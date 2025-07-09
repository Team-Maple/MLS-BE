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
                requiredStats.getLevel(),
                requiredStats.getStr(),
                requiredStats.getDex(),
                requiredStats.getIntelligence(),
                requiredStats.getLuk(),
                requiredStats.getPop()
        );
    }
}