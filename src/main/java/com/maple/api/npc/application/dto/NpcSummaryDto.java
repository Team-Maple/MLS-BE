package com.maple.api.npc.application.dto;

import com.maple.api.npc.domain.Npc;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "NPC 요약 정보")
public record NpcSummaryDto(
        @Schema(description = "NPC ID", example = "2000")
        Integer npcId,
        
        @Schema(description = "NPC 이름", example = "로저")
        String name,
        
        @Schema(description = "NPC 이미지 URL", example = "https://maplestory.io/api/gms/62/npc/2000/icon?resize=2")
        String imageUrl,
        
        @Schema(description = "데이터 타입", example = "npc")
        String type
) {
    public static NpcSummaryDto toDto(Npc entity) {
        return new NpcSummaryDto(entity.getNpcId(), entity.getNameKr(), entity.getIconUrlDetail(), "npc");
    }
}