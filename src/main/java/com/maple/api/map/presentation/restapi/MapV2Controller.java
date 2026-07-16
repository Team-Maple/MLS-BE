package com.maple.api.map.presentation.restapi;

import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.common.presentation.restapi.ResponseTemplate;
import com.maple.api.map.application.dto.MapRecommendationV2Dto;
import com.maple.api.map.application.MapRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/maps")
@RequiredArgsConstructor
@Tag(name = "Map V2", description = "추천 근거 코드를 포함한 v2 맵 API")
@Validated
public class MapV2Controller {

    private final MapRecommendationService mapRecommendationService;

    @GetMapping("/recommendations")
    @Operation(
            summary = "근거 기반 사냥터 추천",
            description = "MySQL의 APPROVED 추천 근거를 요청 Job lineage에 맞춰 합산합니다. " +
                    "reasons는 reward, play_style, operability 순서이며 축별 최대 하나의 안정적인 axis/value 코드만 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사냥터 추천 결과 조회 성공"),
            @ApiResponse(responseCode = "400", description = "레벨 또는 limit validation 실패"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 직업"),
            @ApiResponse(
                    responseCode = "503",
                    description = "v2가 비활성 상태이거나 MySQL 추천 인프라/schema/시간 제한을 사용할 수 없음"
            )
    })
    public ResponseEntity<ResponseTemplate<List<MapRecommendationV2Dto>>> recommendMaps(
            @Parameter(description = "요청 캐릭터 레벨", example = "100")
            @RequestParam @Min(1) @Max(200) int level,
            @Parameter(description = "요청 직업 ID", example = "100")
            @RequestParam int jobId,
            @Parameter(
                    description = "최종 정렬 뒤 적용할 반환 개수",
                    example = "5",
                    schema = @Schema(
                            type = "integer",
                            format = "int32",
                            defaultValue = "5",
                            minimum = "1",
                            maximum = "20"
                    )
            )
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) Integer limit,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        String memberId = principalDetails != null ? principalDetails.getProviderId() : null;
        List<MapRecommendationV2Dto> recommendations = mapRecommendationService.recommendV2(memberId, level, jobId, limit);
        return ResponseEntity.ok(ResponseTemplate.success(recommendations));
    }
}
