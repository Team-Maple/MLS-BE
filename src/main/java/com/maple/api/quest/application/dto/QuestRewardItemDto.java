package com.maple.api.quest.application.dto;

import com.maple.api.quest.domain.QuestRewardItem;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "퀘스트 보상 아이템 DTO")
public record QuestRewardItemDto(
        @Schema(description = "아이템 ID", example = "1912000")
        Integer itemId,
        
        @Schema(description = "아이템명", example = "안장")
        String itemName,
        
        @Schema(description = "수량", example = "1")
        Integer quantity
) {
    public static QuestRewardItemDto toDto(QuestRewardItem item, String itemName) {
        return new QuestRewardItemDto(
                item.getItemId(),
                itemName,
                item.getQuantity()
        );
    }
}