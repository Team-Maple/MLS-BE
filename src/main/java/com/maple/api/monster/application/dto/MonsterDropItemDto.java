package com.maple.api.monster.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "몬스터 드롭 아이템 정보")
public record MonsterDropItemDto(
        @Schema(description = "아이템 ID", example = "2000000")
        Integer itemId,
        
        @Schema(description = "아이템 이름", example = "빨간 포션")
        String itemName,
        
        @Schema(description = "드롭 확률", example = "0.1")
        Double dropRate,

        @Schema(description = "아이템 이미지 URL", example = "https://maplestory.io/api/gms/62/item/2000000/icon?resize=2")
        String imageUrl,
        
        @Schema(description = "아이템 레벨 (장비 아이템은 required_level, 다른 아이템은 0)", example = "10")
        Integer itemLevel
) {
}