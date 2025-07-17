package com.maple.api.npc.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "NPC 검색 요청 정보")
public record NpcSearchRequestDto(
        @Schema(description = "검색 키워드 (NPC 이름)", example = "로저")
        String keyword
) {
}