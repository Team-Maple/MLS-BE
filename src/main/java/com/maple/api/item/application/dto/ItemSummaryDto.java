package com.maple.api.item.application.dto;

import com.maple.api.item.domain.Item;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "아이템 요약 정보 DTO")
public record ItemSummaryDto(
        @Schema(description = "아이템 ID", example = "1002001")
        Integer itemId,
        
        @Schema(description = "아이템명", example = "메탈 기어")
        String name,
        
        @Schema(description = "아이템 이미지 URL", example = "https://maplestory.io/api/gms/62/item/1002001/icon?resize=2")
        String imageUrl,
        
        @Schema(description = "아이템 타입 (고정값: 'item')", example = "item")
        String type,

        @Schema(description = "로그인 사용자의 북마크 여부", example = "false")
        boolean isBookmarked
) {
    public static ItemSummaryDto toDto(Item entity) {
        return toDto(entity, false);
    }

    public static ItemSummaryDto toDto(Item entity, boolean isBookmarked) {
        return new ItemSummaryDto(
                entity.getItemId(),
                entity.getNameKr(),
                entity.getItemImageUrl(),
                "item",
                isBookmarked
        );
    }
}
