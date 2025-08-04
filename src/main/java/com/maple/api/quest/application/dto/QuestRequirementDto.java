package com.maple.api.quest.application.dto;

import com.maple.api.quest.domain.QuestRequirement;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "퀘스트 완료 조건 DTO")
public record QuestRequirementDto(
        @Schema(description = "완료 조건 타입(item, monster)", example = "item")
        String requirementType,
        
        @Schema(description = "아이템 ID", example = "4031507")
        Integer itemId,
        
        @Schema(description = "아이템명", example = "페로몬")
        String itemName,
        
        @Schema(description = "몬스터 ID", example = "null")
        Integer monsterId,
        
        @Schema(description = "몬스터명", example = "null")
        String monsterName,
        
        @Schema(description = "수량", example = "5")
        Integer quantity
) {
    public static QuestRequirementDto toDto(QuestRequirement requirement, String itemName, String monsterName) {
        return new QuestRequirementDto(
                requirement.getRequirementType(),
                requirement.getItemId(),
                itemName,
                requirement.getMonsterId(),
                monsterName,
                requirement.getQuantity()
        );
    }
}