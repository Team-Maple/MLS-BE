package com.maple.api.quest.application.dto;

import com.maple.api.quest.domain.QuestReward;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "퀘스트 보상 정보 DTO")
public record QuestRewardDto(
        @Schema(description = "경험치", example = "1000")
        Long exp,
        
        @Schema(description = "메소", example = "5000")
        Long meso,
        
        @Schema(description = "인기도", example = "10")
        Integer popularity
) {
    public static QuestRewardDto toDto(QuestReward reward) {
        return new QuestRewardDto(
                reward.getExp(),
                reward.getMeso(),
                reward.getPopularity()
        );
    }
}