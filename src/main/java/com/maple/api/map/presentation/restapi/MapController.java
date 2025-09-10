package com.maple.api.map.presentation.restapi;

import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.map.application.MapService;
import com.maple.api.map.application.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
            @ParameterObject @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        String memberId = principalDetails != null ? principalDetails.getProviderId() : null;
        Page<MapSummaryDto> maps = mapService.searchMaps(memberId, request, pageable);
        return ResponseEntity.ok(maps);
    }

    @GetMapping("/{mapId}")
    @Operation(
        summary = "맵 상세 조회",
        description = "특정 맵의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "맵 상세 정보 조회 성공"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 맵"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<MapDetailDto> getMapDetail(@PathVariable Integer mapId,
                                                     @org.springframework.security.core.annotation.AuthenticationPrincipal PrincipalDetails principalDetails) {
        String memberId = principalDetails != null ? principalDetails.getProviderId() : null;
        MapDetailDto mapDetail = mapService.getMapDetail(memberId, mapId);
        return ResponseEntity.ok(mapDetail);
    }

    @GetMapping("/{mapId}/monsters")
    @Operation(
        summary = "맵 몬스터 리스트 조회",
        description = "특정 맵에서 출현하는 몬스터 리스트를 조회합니다.\n\n" +
                     "**정렬 옵션:**\n" +
                     "- `level`: 몬스터 레벨 기준\n" +
                     "- `maxSpawnCount`: 최대 스폰 수 기준\n\n" +
                     "**사용 예시:**\n" +
                     "- `?sort=level,desc`: 레벨 내림차순\n" +
                     "- `?sort=maxSpawnCount,asc`: 스폰 수 오름차순"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "몬스터 리스트 조회 성공"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 맵"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<MapMonsterDto>> getMapMonsters(
            @PathVariable Integer mapId,
            @ParameterObject Sort sort) {
        List<MapMonsterDto> monsters = mapService.getMapMonsters(mapId, sort);
        return ResponseEntity.ok(monsters);
    }

    @GetMapping("/{mapId}/npcs")
    @Operation(
        summary = "맵 NPC 리스트 조회",
        description = "특정 맵에 있는 NPC 리스트를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "NPC 리스트 조회 성공"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 맵"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<MapNpcDto>> getMapNpcs(@PathVariable Integer mapId) {
        List<MapNpcDto> npcs = mapService.getMapNpcs(mapId);
        return ResponseEntity.ok(npcs);
    }
}
