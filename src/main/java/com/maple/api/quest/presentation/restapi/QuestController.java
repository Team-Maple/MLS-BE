package com.maple.api.quest.presentation.restapi;

import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.common.presentation.restapi.CountResponse;
import com.maple.api.common.presentation.restapi.ResponseTemplate;
import com.maple.api.quest.application.QuestService;
import com.maple.api.quest.application.dto.QuestChainResponseDto;
import com.maple.api.quest.application.dto.QuestDetailDto;
import com.maple.api.quest.application.dto.QuestSearchRequestDto;
import com.maple.api.quest.application.dto.QuestSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ResponseEntity<ResponseTemplate<Page<QuestSummaryDto>>> searchQuests(
            @ParameterObject QuestSearchRequestDto request,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        String memberId = principalDetails != null ? principalDetails.getProviderId() : null;
        Page<QuestSummaryDto> quests = questService.searchQuests(memberId, request, pageable);
        return ResponseEntity.ok(ResponseTemplate.success(quests));
    }

    @GetMapping("/counts")
    @Operation(summary = "퀘스트 검색 결과 수 조회",
            description = "키워드로 필터링된 퀘스트 데이터의 총 개수를 조회합니다.")
    public ResponseEntity<ResponseTemplate<CountResponse>> countQuests(
            @Parameter(description = "검색할 키워드", example = "첫 번째")
            @RequestParam(required = false) String keyword) {
        long counts = questService.countQuestsByKeyword(keyword);
        return ResponseEntity.ok(ResponseTemplate.success(CountResponse.of(counts)));
    }

    @GetMapping("/{questId}")
    @Operation(
        summary = "퀘스트 상세 조회",
        description = "퀘스트의 상세 정보를 조회합니다. 퀘스트 기본 정보, 보상, 완료 조건, 허용 직업 등의 정보를 포함합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "퀘스트 상세 정보 조회 성공"),
        @ApiResponse(responseCode = "404", description = "퀘스트를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseTemplate<QuestDetailDto>> getQuestDetail(@PathVariable Integer questId,
                                                         @org.springframework.security.core.annotation.AuthenticationPrincipal PrincipalDetails principalDetails) {
        String memberId = principalDetails != null ? principalDetails.getProviderId() : null;
        QuestDetailDto questDetail = questService.getQuestDetail(memberId, questId);
        return ResponseEntity.ok(ResponseTemplate.success(questDetail));
    }

    @GetMapping("/{questId}/chain")
    @Operation(
        summary = "연계 퀘스트 조회",
        description = "특정 퀘스트의 연계 퀘스트 정보를 조회합니다. " +
                     "선행 완료 퀘스트와 퀘스트 완료 시 열리는 퀘스트 목록을 제공합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "연계 퀘스트 조회 성공"),
        @ApiResponse(responseCode = "404", description = "퀘스트를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseTemplate<QuestChainResponseDto>> getQuestChain(@PathVariable Integer questId) {
        QuestChainResponseDto questChain = questService.getQuestChain(questId);
        return ResponseEntity.ok(ResponseTemplate.success(questChain));
    }
}
