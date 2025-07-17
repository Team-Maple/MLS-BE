package com.maple.api.quest.presentation.restapi;

import com.maple.api.quest.application.QuestService;
import com.maple.api.quest.application.dto.QuestSearchRequestDto;
import com.maple.api.quest.application.dto.QuestSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quests")
@RequiredArgsConstructor
@Tag(name = "Quest", description = "퀘스트 관련 API")
public class QuestController {

    private final QuestService questService;

    @GetMapping
    @Operation(
        summary = "퀘스트 검색",
        description = "다양한 조건으로 퀘스트를 검색합니다. 퀘스트 이름으로 필터링할 수 있습니다.\n\n" +
                     "**정렬 기준:**\n" +
                     "- 기본 정렬 적용\n" +
                     "- 페이지 크기: 20개 (기본값)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "퀘스트 검색 결과 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Page<QuestSummaryDto>> searchQuests(
            @ParameterObject QuestSearchRequestDto request,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<QuestSummaryDto> quests = questService.searchQuests(request, pageable);
        return ResponseEntity.ok(quests);
    }
}