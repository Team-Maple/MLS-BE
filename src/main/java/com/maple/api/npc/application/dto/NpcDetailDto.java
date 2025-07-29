package com.maple.api.npc.application.dto;

import com.maple.api.npc.domain.Npc;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "NPC 상세 정보 응답")
public record NpcDetailDto(
        @Schema(description = "NPC ID", example = "9000000")
        Integer npcId,
        
        @Schema(description = "NPC 한국어 이름", example = "아르바이트 매니저")
        String nameKr,
        
        @Schema(description = "NPC 영어 이름", example = "Part-time Job Manager")
        String nameEn,
        
        @Schema(description = "NPC 상세 아이콘 URL", example = "https://maplestory.io/api/gms/62/npc/9000000/icon")
        String iconUrlDetail
) {
    public static NpcDetailDto toDto(Npc npc) {
        return new NpcDetailDto(
                npc.getNpcId(),
                npc.getNameKr(),
                npc.getNameEn(),
                npc.getIconUrlDetail()
        );
    }
}