package com.maple.api.auth.application.dto;

import com.maple.api.auth.domain.Provider;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.lang.Nullable;

@Schema(description = "로컬 환경에서 사용하는 테스트 로그인 요청")
public record TestLoginRequestDto(
        @Schema(description = "프로바이더 식별자(일반적으로 social id)", example = "test-user-1")
        String providerId,
        @Schema(description = "소셜 프로바이더 (기본값: KAKAO)", example = "KAKAO")
        Provider provider,
        @Nullable
        @Schema(description = "닉네임 (없으면 랜덤 생성)")
        String nickname
) {
}
