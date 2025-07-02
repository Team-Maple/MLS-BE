package com.maple.api.npc.application.dto;

import com.maple.api.npc.domain.Npc;

public record NpcSummaryDto(Integer npcId, String name, String imageUrl, String type) {
    public static NpcSummaryDto toDto(Npc entity) {
        return new NpcSummaryDto(entity.getNpcId(), entity.getNameKr(), entity.getIconUrlDetail(), "npc");
    }
}