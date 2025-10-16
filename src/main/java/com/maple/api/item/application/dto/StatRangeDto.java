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

        Integer base = nullIfZero(statRange.getBase());
        Integer min = nullIfZero(statRange.getMin());
        Integer max = nullIfZero(statRange.getMax());

        if (base == null && min == null && max == null) {
            return null;
        }

        return new StatRangeDto(base, min, max);
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
