package com.maple.api.quest.application.dto;

import com.maple.api.quest.domain.Quest;

public record QuestSummaryDto(Integer questId, String name, String imageUrl, String type) {
    public static QuestSummaryDto toDto(Quest entity) {
        return new QuestSummaryDto(entity.getQuestId(), entity.getNameKr(), entity.getIconUrl(), "quest");
    }
}