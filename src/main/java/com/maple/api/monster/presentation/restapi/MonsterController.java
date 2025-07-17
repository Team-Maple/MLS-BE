package com.maple.api.monster.presentation.restapi;

import com.maple.api.monster.application.MonsterService;
import com.maple.api.monster.application.dto.MonsterDetailDto;
import com.maple.api.monster.application.dto.MonsterSearchRequestDto;
import com.maple.api.monster.application.dto.MonsterSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Monster", description = "몬스터 관련 API")
@RestController
@RequestMapping("/api/v1/monsters")
@RequiredArgsConstructor
public class MonsterController {

    private final MonsterService monsterService;

    @Operation(summary = "몬스터 검색", description = "다양한 조건으로 몬스터를 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 조회됨", 
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터", 
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", 
                    content = @Content)
    })
    @GetMapping
    public ResponseEntity<Page<MonsterSummaryDto>> searchMonsters(
            @Valid @ParameterObject MonsterSearchRequestDto request,
            @PageableDefault(size = 20, sort = "monsterId") Pageable pageable) {
        return ResponseEntity.ok(monsterService.searchMonsters(request, pageable));
    }

    @Operation(summary = "몬스터 상세 조회", description = "특정 몬스터의 상세 정보를 조회합니다. 스폰 맵과 드롭 아이템 정보가 포함됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 조회됨", 
                    content = @Content(schema = @Schema(implementation = MonsterDetailDto.class))),
            @ApiResponse(responseCode = "404", description = "몬스터를 찾을 수 없음",
                    // TODO: Schema 추가
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", 
                    content = @Content)
    })
    @GetMapping("/{monsterId}")
    public ResponseEntity<MonsterDetailDto> getMonsterDetail(
            @Parameter(description = "몬스터 ID", required = true, example = "100100")
            @PathVariable Integer monsterId) {
        return ResponseEntity.ok(monsterService.getMonsterDetail(monsterId));
    }
}