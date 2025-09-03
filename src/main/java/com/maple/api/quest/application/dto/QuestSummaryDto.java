package com.maple.api.quest.application.dto;

import com.maple.api.quest.domain.Quest;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "퀘스트 요약 정보")
public record QuestSummaryDto(
        @Schema(description = "퀘스트 ID", example = "1000")
        Integer questId,
        
        @Schema(description = "퀘스트 이름", example = "세라에게 거울 빌려오기")
        String name,
        
        @Schema(description = "퀘스트 이미지 URL", example = "https://maplestory.io/api/gms/62/quest/1000/icon?resize=2")
        String imageUrl,
        
        @Schema(description = "데이터 타입 (고정값: 'quest')", example = "quest")
        String type,
        
        @Schema(description = "로그인 사용자의 북마크 여부", example = "false")
        boolean isBookmarked
) {
    public static QuestSummaryDto toDto(Quest entity) {
        return toDto(entity, false);
    }

    public static QuestSummaryDto toDto(Quest entity, boolean isBookmarked) {
        return new QuestSummaryDto(entity.getQuestId(), entity.getNameKr(), entity.getIconUrl(), "quest", isBookmarked);
    }
}
