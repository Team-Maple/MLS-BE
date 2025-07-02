package com.maple.api.quest.presentation.restapi;

import com.maple.api.quest.application.QuestService;
import com.maple.api.quest.application.dto.QuestSearchRequestDto;
import com.maple.api.quest.application.dto.QuestSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quests")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;

    @GetMapping
    public ResponseEntity<Page<QuestSummaryDto>> searchQuests(
            @ModelAttribute QuestSearchRequestDto request,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<QuestSummaryDto> quests = questService.searchQuests(request, pageable);
        return ResponseEntity.ok(quests);
    }
}