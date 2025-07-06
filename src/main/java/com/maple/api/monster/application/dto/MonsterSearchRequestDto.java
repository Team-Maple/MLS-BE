package com.maple.api.monster.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record MonsterSearchRequestDto(
        String keyword,
        @Min(1)
        Integer minLevel,
        @Max(200)
        Integer maxLevel
) {
}
