package com.maple.api.item.application.dto;

import com.maple.api.item.domain.StatRange;

public record StatRangeDto(
        Integer base,
        Integer min,
        Integer max
) {
    public static StatRangeDto toDto(StatRange statRange) {
        if (statRange == null) {
            return null;
        }
        
        return new StatRangeDto(
                statRange.getBase(),
                statRange.getMin(),
                statRange.getMax()
        );
    }
}