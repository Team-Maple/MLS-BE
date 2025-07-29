package com.maple.api.npc.presentation.restapi;

import com.maple.api.common.presentation.exception.ExceptionResponse;
import com.maple.api.npc.application.NpcService;
import com.maple.api.npc.application.dto.NpcDetailDto;
import com.maple.api.npc.application.dto.NpcQuestDto;
import com.maple.api.npc.application.dto.NpcSearchRequestDto;
import com.maple.api.npc.application.dto.NpcSpawnMapDto;
import com.maple.api.npc.application.dto.NpcSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/npcs")
@RequiredArgsConstructor
@Tag(name = "NPC", description = "NPC 관련 API")
public class NpcController {

    private final NpcService npcService;

    @GetMapping
    @Operation(
        summary = "NPC 검색",
        description = "다양한 조건으로 NPC를 검색합니다. NPC 이름으로 필터링할 수 있습니다.\n\n" +
                     "**정렬 기준:**\n" +
                     "- 기본 정렬 적용\n" +
                     "- 페이지 크기: 20개 (기본값)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "NPC 검색 결과 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Page<NpcSummaryDto>> searchNpcs(
            @ParameterObject NpcSearchRequestDto request,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<NpcSummaryDto> npcs = npcService.searchNpcs(request, pageable);
        return ResponseEntity.ok(npcs);
    }

    @GetMapping("/{npcId}")
    @Operation(
            summary = "NPC 상세 조회",
            description = "특정 NPC의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 조회됨", 
                    content = @Content(schema = @Schema(implementation = NpcDetailDto.class))),
            @ApiResponse(responseCode = "404", description = "NPC를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public ResponseEntity<NpcDetailDto> getNpcDetail(
            @Parameter(description = "NPC ID", required = true, example = "1010100")
            @PathVariable Integer npcId) {
        return ResponseEntity.ok(npcService.getNpcDetail(npcId));
    }

    @GetMapping("/{npcId}/maps")
    @Operation(
        summary = "NPC 출현 맵 리스트 조회",
        description = "특정 NPC가 출현하는 맵 리스트를 조회합니다.\n\n"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "출현 맵 리스트 조회 성공"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 NPC"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<NpcSpawnMapDto>> getNpcSpawnMaps(
            @Parameter(description = "NPC ID", required = true, example = "1010100")
            @PathVariable Integer npcId) {
        List<NpcSpawnMapDto> spawnMaps = npcService.getNpcSpawnMaps(npcId);
        return ResponseEntity.ok(spawnMaps);
    }

    @GetMapping("/{npcId}/quests")
    @Operation(
        summary = "NPC 퀘스트 리스트 조회",
        description = "특정 NPC가 제공하는 퀘스트 리스트를 조회합니다.\n\n" +
                     "**정렬 옵션:**\n" +
                     "- `minLevel`: 최소 수락 레벨 기준 (기본값: 오름차순)\n" +
                     "- `maxLevel`: 최대 수락 레벨 기준\n\n" +
                     "**사용 예시:**\n" +
                     "- `?sort=minLevel,asc`: 최소 레벨 낮은 순 (기본값)\n" +
                     "- `?sort=minLevel,desc`: 최소 레벨 높은 순\n" +
                     "- `?sort=maxLevel,asc`: 최대 레벨 오름차순"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "퀘스트 리스트 조회 성공"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 NPC"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<NpcQuestDto>> getNpcQuests(
            @Parameter(description = "NPC ID", required = true, example = "1010100")
            @PathVariable Integer npcId,
            @ParameterObject Sort sort) {
        List<NpcQuestDto> quests = npcService.getNpcQuests(npcId, sort);
        return ResponseEntity.ok(quests);
    }
}