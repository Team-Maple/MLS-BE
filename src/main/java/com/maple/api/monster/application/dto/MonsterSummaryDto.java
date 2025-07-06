package com.maple.api.monster.application.dto;

import com.maple.api.monster.domain.Monster;

public record MonsterSummaryDto(
        Integer monsterId,
        String name,
        String imageUrl,
        String type
) {
    public static MonsterSummaryDto toDto(Monster entity) {
        return new MonsterSummaryDto(
                entity.getMonsterId(),
                entity.getNameKr(),
                entity.getImageUrl(),
                "monster"
        );
    }
}
