package com.maple.api.bookmark.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "몬스터 북마크 검색 요청 DTO")
public record MonsterBookmarkSearchRequestDto(
        @Schema(
            description = "최소 레벨 (1-200)",
            example = "10",
            minimum = "1",
            nullable = true
        )
        @Min(value = 1, message = "최소 레벨은 1 이상이어야 합니다")
        Integer minLevel,
        
        @Schema(
            description = "최대 레벨 (1-200)",
            example = "100",
            maximum = "200",
            nullable = true
        )
        @Max(value = 200, message = "최대 레벨은 200 이하여야 합니다")
        Integer maxLevel
) {
}