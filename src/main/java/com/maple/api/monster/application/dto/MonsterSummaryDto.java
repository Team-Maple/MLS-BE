package com.maple.api.monster.application.dto;

import com.maple.api.monster.domain.Monster;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "몬스터 요약 정보")
public record MonsterSummaryDto(
        @Schema(description = "몬스터 ID", example = "100001")
        Integer monsterId,
        
        @Schema(description = "몬스터 이름", example = "달팽이")
        String name,
        
        @Schema(description = "몬스터 이미지 URL", example = "https://maplestory.io/api/gms/62/mob/100100/render/stand")
        String imageUrl,
        
        @Schema(description = "타입 (고정값: monster)", example = "monster")
        String type,
        
        @Schema(description = "로그인 사용자의 북마크 여부", example = "false")
        boolean isBookmarked
) {
    public static MonsterSummaryDto toDto(Monster entity) {
        return toDto(entity, false);
    }

    public static MonsterSummaryDto toDto(Monster entity, boolean isBookmarked) {
        return new MonsterSummaryDto(
                entity.getMonsterId(),
                entity.getNameKr(),
                entity.getImageUrl(),
                "monster",
                isBookmarked
        );
    }
}
