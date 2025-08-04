package com.maple.api.quest.application.dto;

import com.maple.api.quest.domain.Quest;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "연계 퀘스트 정보")
public record QuestChainDto(
        @Schema(description = "퀘스트 ID", example = "1001")
        Integer questId,
        
        @Schema(description = "퀘스트 이름", example = "히나에게 거울 가져가기")
        String name,
        
        @Schema(description = "퀘스트 수락 최소 레벨", example = "0")
        Integer minLevel,
        
        @Schema(description = "퀘스트 수락 최대 레벨", example = "null")
        Integer maxLevel,
        
        @Schema(description = "퀘스트 아이콘 URL", example = "https://maplestory.io/api/gms/62/quest/1001/icon?resize=2")
        String iconUrl
) {
    public static QuestChainDto toDto(Quest quest) {
        return new QuestChainDto(
                quest.getQuestId(),
                quest.getNameKr(),
                quest.getMinLevel(),
                quest.getMaxLevel(),
                quest.getIconUrl()
        );
    }
}