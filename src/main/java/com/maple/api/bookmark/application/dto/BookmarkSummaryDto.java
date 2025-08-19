package com.maple.api.bookmark.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "북마크 요약 정보")
public record BookmarkSummaryDto(
        @Schema(description = "원본 ID (각 타입별 고유 ID)", example = "1002001")
        Integer originalId,
        
        @Schema(description = "이름", example = "메탈 기어")
        String name,
        
        @Schema(description = "이미지 URL", example = "https://maplestory.io/api/gms/62/item/1002001/icon?resize=2")
        String imageUrl,
        
        @Schema(description = "타입 (ITEM, MONSTER, QUEST, NPC, MAP)", example = "ITEM")
        String type,
        
        @Schema(description = "레벨", example = "0")
        Integer level
) {
}