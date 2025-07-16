package com.maple.api.map.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "맵 검색 요청 정보")
public record MapSearchRequestDto(
        @Schema(description = "검색 키워드 (맵 이름)", example = "헤네시스")
        String keyword
) {
}