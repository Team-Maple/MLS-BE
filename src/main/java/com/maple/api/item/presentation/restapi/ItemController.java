package com.maple.api.item.presentation.restapi;

import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.item.application.ItemService;
import com.maple.api.item.application.dto.ItemDetailDto;
import com.maple.api.item.application.dto.ItemMonsterDropDto;
import com.maple.api.item.application.dto.ItemSearchRequestDto;
import com.maple.api.item.application.dto.ItemSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
@Tag(name = "Item", description = "아이템 관련 API")
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    @Operation(
        summary = "아이템 검색",
        description = "다양한 조건으로 아이템을 검색합니다. 키워드, 직업, 레벨 범위, 카테고리 등으로 필터링할 수 있으며, 정렬 옵션을 제공합니다.\n\n" +
                     "**정렬 가능한 필드:**\n" +
                     "- name: 아이템명\n" +
                     "- level: 요구레벨\n" +
                     "- itemId: 아이템ID\n\n" +
                     "**정렬 사용 예시:**\n" +
                     "- `sort=name,asc` (아이템명 오름차순)\n" +
                     "- `sort=level,desc` (레벨 내림차순)\n" +
                     "- `sort=name,asc,level,desc` (아이템명 오름차순, 레벨 내림차순)"
    )
    public ResponseEntity<Page<ItemSummaryDto>> searchItems(
            @Valid @ParameterObject ItemSearchRequestDto searchRequest,
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        String memberId = principalDetails != null ? principalDetails.getProviderId() : null;
        Page<ItemSummaryDto> results = itemService.searchItems(memberId, searchRequest, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "아이템 상세 조회",
        description = "아이템 ID로 특정 아이템의 상세 정보를 조회합니다.\n\n" +
                "장비 아이템의 경우 스탯 정보,\n\n" +
                "주문서 아이템의 경우 스크롤 정보 등을 포함합니다."
    )
    public ResponseEntity<ItemDetailDto> getItemDetail(
            @Parameter(description = "아이템 ID", example = "1302000", required = true)
            @PathVariable Integer id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal PrincipalDetails principalDetails) {
        String memberId = principalDetails != null ? principalDetails.getProviderId() : null;
        ItemDetailDto itemDetail = itemService.getItemDetail(memberId, id);
        return ResponseEntity.ok(itemDetail);
    }

    @GetMapping("/{itemId}/monsters")
    @Operation(
        summary = "아이템 드롭 몬스터 리스트 조회",
        description = "특정 아이템을 드롭하는 몬스터 리스트를 조회합니다.\n\n" +
                     "**정렬 옵션:**\n" +
                     "- `dropRate`: 드롭율 기준\n" +
                     "- `level`: 몬스터 레벨 기준\n" +
                     "- `monsterId`: 몬스터 ID 기준\n\n" +
                     "**사용 예시:**\n" +
                     "- `?sort=dropRate,desc`: 드롭율 내림차순\n" +
                     "- `?sort=level,asc`: 레벨 오름차순\n" +
                     "- `?sort=level,desc`: 레벨 내림차순"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "드롭 몬스터 리스트 조회 성공"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 아이템"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<ItemMonsterDropDto>> getItemDropMonsters(
            @Parameter(description = "아이템 ID", example = "2070005", required = true)
            @PathVariable Integer itemId,
            @ParameterObject Sort sort) {
        List<ItemMonsterDropDto> dropMonsters = itemService.getItemDropMonsters(itemId, sort);
        return ResponseEntity.ok(dropMonsters);
    }
}
