package com.maple.api.quest.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "연계 퀘스트 조회 응답")
public record QuestChainResponseDto(
        @Schema(description = "선행 완료 퀘스트 목록")
        List<QuestChainDto> previousQuests,
        
        @Schema(description = "퀘스트 완료 시 열리는 퀘스트 목록")
        List<QuestChainDto> nextQuests
) {
    public static QuestChainResponseDto of(List<QuestChainDto> previousQuests, List<QuestChainDto> nextQuests) {
        return new QuestChainResponseDto(previousQuests, nextQuests);
    }
}