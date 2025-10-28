package com.maple.api.quest.repository;

import com.maple.api.quest.application.dto.QuestSearchRequestDto;
import com.maple.api.quest.domain.Quest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestQueryDslRepository {
    Page<Quest> searchQuests(QuestSearchRequestDto request, Pageable pageable);
    long countQuestsByKeyword(String keyword);
}
