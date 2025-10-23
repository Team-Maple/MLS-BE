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

        @Schema(description = "몬스터 레벨", example = "3")
        Integer level,

        @Schema(description = "타입 (고정값: monster)", example = "monster")
        String type,

        @Schema(description = "로그인 사용자가 생성한 북마크 ID (없으면 null)", example = "123")
        Integer bookmarkId
) {
    public static MonsterSummaryDto toDto(Monster entity) {
        return toDto(entity, null);
    }

    public static MonsterSummaryDto toDto(Monster entity, Integer bookmarkId) {
        return new MonsterSummaryDto(
                entity.getMonsterId(),
                entity.getNameKr(),
                entity.getImageUrl(),
                entity.getLevel(),
                "monster",
                bookmarkId
        );
    }
}
