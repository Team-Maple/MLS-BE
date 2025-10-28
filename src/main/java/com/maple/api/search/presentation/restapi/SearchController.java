package com.maple.api.search.presentation.restapi;

import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.common.presentation.restapi.CountResponse;
import com.maple.api.common.presentation.restapi.ResponseTemplate;
import com.maple.api.search.application.SearchService;
import com.maple.api.search.application.dto.SearchSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "통합 검색 API")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(
        summary = "통합 검색",
        description = "키워드를 이용하여 아이템, 몬스터, 퀘스트, NPC, 맵 등의 정보를 통합적으로 검색합니다. " +
                     "키워드가 없으면 전체 데이터를 반환합니다.\n\n" +
                     "**검색 가능한 항목:**\n" +
                     "- 아이템 (Item)\n" +
                     "- 몬스터 (Monster)\n" +
                     "- 퀘스트 (Quest)\n" +
                     "- NPC\n" +
                     "- 맵 (Map)\n\n" +
                     "**정렬 기준:**\n" +
                     "- name: 이름순 정렬 (기본값)\n" +
                     "- 페이지 크기: 20개 (기본값)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "검색 결과 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (키워드 길이 초과 등)"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseTemplate<Page<SearchSummaryDto>>> search(
            @Parameter(description = "검색할 키워드 (최대 100자)", example = "슬라임")
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        String memberId = principalDetails != null ? principalDetails.getProviderId() : null;
        Page<SearchSummaryDto> results = searchService.search(memberId, keyword, pageable);
        return ResponseEntity.ok(ResponseTemplate.success(results));
    }

    @GetMapping("/counts")
    @Operation(summary = "통합 검색 결과 수 조회",
            description = "키워드로 필터링된 통합 검색 결과의 총 개수를 조회합니다.")
    public ResponseEntity<ResponseTemplate<CountResponse>> count(
            @Parameter(description = "검색할 키워드 (최대 100자)", example = "슬라임")
            @RequestParam(required = false) @Size(max = 100) String keyword) {
        long counts = searchService.countByKeyword(keyword);
        return ResponseEntity.ok(ResponseTemplate.success(CountResponse.of(counts)));
    }
}
