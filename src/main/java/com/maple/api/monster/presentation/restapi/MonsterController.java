package com.maple.api.monster.presentation.restapi;

import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.common.presentation.exception.ExceptionResponse;
import com.maple.api.common.presentation.restapi.ResponseTemplate;
import com.maple.api.monster.application.MonsterService;
import com.maple.api.monster.application.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

@Tag(name = "Monster", description = "몬스터 관련 API")
@RestController
@RequestMapping("/api/v1/monsters")
@RequiredArgsConstructor
@Tag(name = "Monster", description = "몬스터 관련 API")
public class MonsterController {

    private final MonsterService monsterService;

    @GetMapping
    @Operation(
        summary = "몬스터 검색",
        description = "다양한 조건으로 몬스터를 검색합니다. 이름으로 필터링할 수 있습니다.\n\n" +
                     "**정렬 기준:**\n" +
                     "- monsterId: 몬스터 ID 순 정렬 (기본값)\n" +
                     "- name, level, exp 등 다양한 필드로 정렬 가능\n\n" +
                     "**정렬 사용 예시:**\n" +
                     "- `sort=name,asc` (몬스터 이름 오름차순)\n"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "몬스터 검색 결과 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseTemplate<Page<MonsterSummaryDto>>> searchMonsters(
            @Valid @ParameterObject MonsterSearchRequestDto request,
            @ParameterObject @PageableDefault(size = 20, sort = "monsterId") Pageable pageable,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        String memberId = principalDetails != null ? principalDetails.getProviderId() : null;
        return ResponseEntity.ok(ResponseTemplate.success(monsterService.searchMonsters(memberId, request, pageable)));
    }

    @GetMapping("/{monsterId}")
    @Operation(
            summary = "몬스터 상세 조회",
            description = "특정 몬스터의 상세 정보를 조회합니다. 스폰 맵과 드롭 아이템 정보가 포함됩니다.\n\n" +
                    "**typeEffectiveness 설명:**\n" +
                    "- `IMMUNE`: 면역\n" +
                    "- `RESIST`: 저항\n" +
                    "- `WEAK`: 약점\n"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 조회됨", 
                    content = @Content(schema = @Schema(implementation = MonsterDetailDto.class))),
            @ApiResponse(responseCode = "404", description = "몬스터를 찾을 수 없음",
                    // TODO: Schema 추가
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public ResponseEntity<ResponseTemplate<MonsterDetailDto>> getMonsterDetail(
            @Parameter(description = "몬스터 ID", required = true, example = "100100")
            @PathVariable Integer monsterId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal PrincipalDetails principalDetails) {
        String memberId = principalDetails != null ? principalDetails.getProviderId() : null;
        return ResponseEntity.ok(ResponseTemplate.success(monsterService.getMonsterDetail(memberId, monsterId)));
    }

    @GetMapping("/{monsterId}/maps")
    @Operation(
        summary = "몬스터 출현 맵 리스트 조회",
        description = "특정 몬스터가 출현하는 맵 리스트를 조회합니다.\n\n" +
                     "**정렬 옵션:**\n" +
                     "- `maxSpawnCount`: 출현 수 기준 (기본값: 내림차순)\n\n" +
                     "**사용 예시:**\n" +
                     "- `?sort=maxSpawnCount,desc`: 출현 수 내림차순 (기본값)\n"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "출현 맵 리스트 조회 성공"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 몬스터"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseTemplate<List<MonsterSpawnMapDto>>> getMonsterSpawnMaps(
            @Parameter(description = "몬스터 ID", required = true, example = "100100")
            @PathVariable Integer monsterId,
            @ParameterObject Sort sort) {
        List<MonsterSpawnMapDto> spawnMaps = monsterService.getMonsterSpawnMaps(monsterId, sort);
        return ResponseEntity.ok(ResponseTemplate.success(spawnMaps));
    }

    @GetMapping("/{monsterId}/items")
    @Operation(
        summary = "몬스터 드롭 아이템 리스트 조회",
        description = "특정 몬스터가 드롭하는 아이템 리스트를 조회합니다.\n\n" +
                     "**정렬 옵션:**\n" +
                     "- `dropRate`: 드롭율 기준 (기본값: 내림차순)\n" +
                     "- `itemId`: 아이템 ID 기준\n\n" +
                     "**사용 예시:**\n" +
                     "- `?sort=dropRate,desc`: 드롭율 내림차순 (기본값)\n" +
                     "- `?sort=level,asc`: 아이템 레벨 오름차순\n"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "드롭 아이템 리스트 조회 성공"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 몬스터"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseTemplate<List<MonsterDropItemDto>>> getMonsterDropItems(
            @Parameter(description = "몬스터 ID", required = true, example = "100100")
            @PathVariable Integer monsterId,
            @ParameterObject Sort sort) {
        List<MonsterDropItemDto> dropItems = monsterService.getMonsterDropItems(monsterId, sort);
        return ResponseEntity.ok(ResponseTemplate.success(dropItems));
    }
}
