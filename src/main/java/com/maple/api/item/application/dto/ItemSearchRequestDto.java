package com.maple.api.item.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

public record ItemSearchRequestDto(
        String keyword,
        Integer jobId,
        @Min(value = 1, message = "최소 레벨은 1 이상이어야 합니다")
        Integer minLevel,
        @Max(value = 200, message = "최대 레벨은 200 이하여야 합니다")
        Integer maxLevel,
        List<Integer> categoryIds
) {
}