package com.maple.api.bookmark.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

@Schema(description = "아이템 북마크 검색 요청 DTO")
public record ItemBookmarkSearchRequestDto(
        @Schema(
            description = "직업 ID 목록 (특정 직업이 착용 가능한 아이템만 검색)",
            example = "[1, 2, 5]",
            nullable = true
        )
        List<Integer> jobIds,
        
        @Schema(
            description = "최소 요구 레벨 (0-200)",
            example = "10",
            minimum = "0",
            nullable = true
        )
        @Min(value = 0, message = "최소 레벨은 0 이상이어야 합니다")
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
