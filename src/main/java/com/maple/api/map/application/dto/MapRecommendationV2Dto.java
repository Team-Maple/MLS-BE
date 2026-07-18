package com.maple.api.map.application.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "근거 코드가 포함된 v2 사냥터 추천 결과")
public record MapRecommendationV2Dto(
        @Schema(description = "추천 맵 ID", example = "100000000")
        Integer mapId,

        @Schema(
                description = "APPROVED 근거의 signed freshness contribution을 합산한 evidence net score",
                example = "0.95"
        )
        double score,

        @Schema(description = "맵 아이콘 URL", example = "https://maplestory.io/api/gms/62/map/100000000/icon?resize=2")
        String iconUrl,

        @Schema(description = "한국어 맵 이름", example = "헤네시스")
        String nameKr,

        @Schema(description = "로그인 사용자가 생성한 북마크 ID (없으면 null)", example = "123")
        Integer bookmarkId,

        @ArraySchema(
                arraySchema = @Schema(description = "축별 최대 하나인 안정적인 추천 이유 코드. 이유가 없으면 빈 배열"),
                schema = @Schema(implementation = MapRecommendationReasonDto.class)
        )
        List<MapRecommendationReasonDto> reasons
) {
    public MapRecommendationV2Dto {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public static MapRecommendationV2Dto toDto(MapRecommendationResultDto result) {
        return new MapRecommendationV2Dto(
                result.mapId(),
                result.score(),
                result.iconUrl(),
                result.nameKr(),
                result.bookmarkId(),
                result.reasons().stream()
                        .map(MapRecommendationReasonDto::toDto)
                        .toList()
        );
    }
}
