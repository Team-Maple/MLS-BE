package com.maple.api.auth.presentation.restapi;

import com.maple.api.auth.application.AuthService;
import com.maple.api.auth.application.MemberService;
import com.maple.api.auth.application.dto.CreateMemberRequestDto;
import com.maple.api.auth.application.dto.LoginResponseDto;
import com.maple.api.auth.application.dto.MemberDto;
import com.maple.api.auth.application.dto.TestLoginRequestDto;
import com.maple.api.auth.domain.Provider;
import com.maple.api.common.presentation.restapi.ResponseTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Profile("local")
@Tag(name = "AuthTest", description = "로컬 환경 테스트용 로그인 API")
public class AuthTestController {

  private final MemberService memberService;
  private final AuthService authService;

  @Operation(
    summary = "테스트 로그인 (local 전용)",
    description = "로컬 환경에서만 동작하는 테스트 로그인입니다. 회원이 없으면 생성 후 토큰을 발급합니다."
  )
  @PostMapping("/login/test")
  public ResponseEntity<ResponseTemplate<LoginResponseDto>> loginForTest(
    @Valid @RequestBody(required = false) TestLoginRequestDto request
  ) {
    Provider provider = request != null && request.provider() != null ? request.provider() : Provider.KAKAO;
    String providerId = (request != null && request.providerId() != null && !request.providerId().isBlank())
      ? request.providerId()
      : "local-test-" + UUID.randomUUID();
    String nickname = request != null ? request.nickname() : null;

    MemberDto member = memberService.findMember(providerId)
      .orElseGet(() -> memberService.createMember(new CreateMemberRequestDto(
        provider,
        providerId,
        nickname,
        null,
        false
      )));

    return ResponseEntity.ok(ResponseTemplate.success(authService.login(member)));
  }
}
