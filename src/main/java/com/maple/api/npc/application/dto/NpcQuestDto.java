package com.maple.api.npc.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "NPC 퀘스트 정보")
public record NpcQuestDto(
        @Schema(description = "퀘스트 ID", example = "2021")
        Integer questId,
        
        @Schema(description = "퀘스트 한국어 이름", example = "특제 장어구이")
        String questNameKr,

        @Schema(description = "퀘스트 영어 이름", example = "First Steps in Maple Road")
        String questNameEn,

        @Schema(description = "퀘스트 아이콘 URL", example = "https://maplestory.io/api/gms/62/quest/1000/icon")
        String questIconUrl,
        
        @Schema(description = "최소 수락 레벨", example = "1")
        Integer minLevel,
        
        @Schema(description = "최대 수락 레벨", example = "10")
        Integer maxLevel
) {
}