package com.maple.api.quest.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "퀘스트 검색 요청 정보")
public record QuestSearchRequestDto(
        @Schema(description = "검색 키워드 (퀘스트 이름)", example = "세라에게")
        String keyword
) {
}