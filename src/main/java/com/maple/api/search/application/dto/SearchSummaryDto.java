package com.maple.api.search.application.dto;

import com.maple.api.search.domain.VwSearchSummary;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "통합 검색 결과 요약 정보")
public record SearchSummaryDto(
        @Schema(description = "원본 ID (각 타입별 고유 ID)", example = "1002001")
        Integer originalId,
        
        @Schema(description = "이름", example = "메탈 기어")
        String name,
        
        @Schema(description = "이미지 URL", example = "https://maplestory.io/api/gms/62/item/1002001/icon?resize=2")
        String imageUrl,
        
        @Schema(description = "타입 (ITEM, MONSTER, QUEST, NPC, MAP)", example = "ITEM")
        String type
) {
    public static SearchSummaryDto toDto(VwSearchSummary entity) {
        return new SearchSummaryDto(entity.getOriginalId(), entity.getName(), entity.getImageUrl(), entity.getType());
    }
}
