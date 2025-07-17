package com.maple.api.monster.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "몬스터 검색 요청 정보")
public record MonsterSearchRequestDto(
        @Schema(description = "검색 키워드 (몬스터 이름)", example = "달팽이")
        String keyword,
        
        @Schema(description = "최소 레벨", example = "1")
        @Min(1)
        Integer minLevel,
        
        @Schema(description = "최대 레벨", example = "200")
        @Max(200)
        Integer maxLevel
) {
}
