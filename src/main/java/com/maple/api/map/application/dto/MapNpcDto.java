package com.maple.api.map.application.dto;

import com.maple.api.npc.domain.Npc;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "맵 출현 NPC 정보")
public record MapNpcDto(
        @Schema(description = "NPC ID", example = "1010100")
        Integer npcId,
        
        @Schema(description = "NPC 이름", example = "리나")
        String npcName,
        
        @Schema(description = "영문명", example = "Rina")
        String npcNameEn,
        
        @Schema(description = "NPC 아이콘 URL")
        String iconUrl
) {
    public static MapNpcDto toDto(Npc npc) {
        return new MapNpcDto(
                npc.getNpcId(),
                npc.getNameKr(),
                npc.getNameEn(),
                npc.getIconUrlDetail()
        );
    }
}