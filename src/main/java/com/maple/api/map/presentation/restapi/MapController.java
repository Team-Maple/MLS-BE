package com.maple.api.map.presentation.restapi;

import com.maple.api.map.application.MapService;
import com.maple.api.map.application.dto.MapSearchRequestDto;
import com.maple.api.map.application.dto.MapSummaryDto;
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
@RequestMapping("/api/v1/maps")
@RequiredArgsConstructor
@Tag(name = "Map", description = "맵 관련 API")
public class MapController {

    private final MapService mapService;

    @GetMapping
    @Operation(
        summary = "맵 검색",
        description = "다양한 조건으로 맵을 검색합니다. 맵 이름으로 필터링할 수 있습니다.\n\n" +
                     "**정렬 기준:**\n" +
                     "- 기본 정렬 적용\n" +
                     "- 페이지 크기: 20개 (기본값)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "맵 검색 결과 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Page<MapSummaryDto>> searchMaps(
            @ParameterObject MapSearchRequestDto request,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<MapSummaryDto> maps = mapService.searchMaps(request, pageable);
        return ResponseEntity.ok(maps);
    }
}