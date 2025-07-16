package com.maple.api.item.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

@Schema(description = "아이템 검색 요청 DTO")
public record ItemSearchRequestDto(
        @Schema(
            description = "검색 키워드 (아이템명으로 검색)",
            example = "메탈 기어",
            nullable = true
        )
        String keyword,
        
        @Schema(
            description = "직업 ID (특정 직업 착용 가능한 아이템만 검색)",
            example = "5",
            nullable = true
        )
        Integer jobId,
        
        @Schema(
            description = "최소 요구 레벨 (1-200)",
            example = "10",
            minimum = "1",
            nullable = true
        )
        @Min(value = 1, message = "최소 레벨은 1 이상이어야 합니다")
        Integer minLevel,
        
        @Schema(
            description = "최대 요구 레벨 (1-200)",
            example = "100",
            maximum = "200",
            nullable = true
        )
        @Max(value = 200, message = "최대 레벨은 200 이하여야 합니다")
        Integer maxLevel,
        
        @Schema(
            description = "카테고리 ID 목록 (특정 카테고리의 아이템만 검색)",
            example = "[24, 25]",
            nullable = true
        )
        List<Integer> categoryIds
) {
}