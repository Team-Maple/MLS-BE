package com.maple.api.quest.application;

import com.maple.api.quest.application.dto.QuestSearchRequestDto;
import com.maple.api.quest.application.dto.QuestSummaryDto;
import com.maple.api.quest.domain.Quest;
import com.maple.api.quest.repository.QuestQueryDslRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestService {

    private final QuestQueryDslRepository questQueryDslRepository;

    @Transactional(readOnly = true)
    public Page<QuestSummaryDto> searchQuests(QuestSearchRequestDto request, Pageable pageable) {
        Page<Quest> questPage = questQueryDslRepository.searchQuests(request, pageable);
        return questPage.map(QuestSummaryDto::toDto);
    }
}