package com.maple.api.map.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "추천 근거 코드. axis/value 조합은 문서화된 고정 코드만 사용합니다.")
public record MapRecommendationReasonDto(
        @Schema(
                description = "추천 이유 축",
                allowableValues = {"reward", "play_style", "operability"},
                example = "reward"
        )
        String axis,

        @Schema(
                description = "축별 이유 코드: reward=xp|meso|loot, play_style=solo|party|party_quest, operability=fatigue|mobility|budget",
                allowableValues = {"xp", "meso", "loot", "solo", "party", "party_quest", "fatigue", "mobility", "budget"},
                example = "xp"
        )
        String value
) {
}
