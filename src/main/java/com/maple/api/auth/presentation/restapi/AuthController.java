package com.maple.api.auth.presentation.restapi;

import com.maple.api.auth.application.AuthService;
import com.maple.api.auth.application.MemberService;
import com.maple.api.auth.application.KakaoUserInfoClient;
import com.maple.api.auth.application.dto.*;
import com.maple.api.auth.domain.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final MemberService memberService;
  private final AuthService authService;
  private final KakaoUserInfoClient kakaoUserInfoClient;

  @PostMapping("/login/kakao")
  public ResponseEntity<LoginResponseDto> loginWithKakao(@RequestParam("access_token") String accessToken) {
    KakaoUserInfo userInfo = kakaoUserInfoClient.getUserInfo(accessToken);
    if (userInfo == null || userInfo.getId() == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 사용자 정보 조회 실패 시
    }
    String kakaoId = userInfo.getId().toString();

    // 가입된 유저인지 확인
    return memberService.findMember(kakaoId)
      .map(member -> ResponseEntity.ok(
        authService.login(member)
      ))
      .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }

  @PostMapping("/signup/kakao")
  public ResponseEntity<LoginResponseDto> signupWithKakao(
    @RequestParam("access_token") String accessToken,
    @RequestBody CreateMemberRequestDto createMemberRequestDto
  ) {
    KakaoUserInfo userInfo = kakaoUserInfoClient.getUserInfo(accessToken);
    if (userInfo == null || userInfo.getId() == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 사용자 정보 조회 실패 시
    }
    String kakaoId = userInfo.getId().toString();

    Optional<MemberDto> memberDtoOptional = memberService.findMember(kakaoId);
    if (memberDtoOptional.isEmpty()) {
      MemberDto member = memberService.createMember(createMemberRequestDto);
      return ResponseEntity.ok(authService.login(member));
    } else {
      // 그냥 로그인 성공처리
      return ResponseEntity.ok(authService.login(memberDtoOptional.get()));
    }
  }


  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@AuthenticationPrincipal PrincipalDetails principalDetails) {
    authService.logout(principalDetails.getMember().getId());
    return ResponseEntity.noContent().build();
  }


  @PostMapping("/reissue")
  public ResponseEntity<TokenResponseDto> reissue(@RequestHeader("refresh_token") String refreshToken) {
    return authService.reissue(refreshToken)
      .map(ResponseEntity::ok)
      .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }

  @PutMapping("/member")
  public ResponseEntity<Void> updateMyInfo(
    @RequestBody UpdateMemberRequestDto request
  ) {
    memberService.updateMember(request);
    return ResponseEntity.ok().build();
  }


  @DeleteMapping("/member")
  public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal PrincipalDetails principalDetails) {
    String userId = principalDetails.getMember().getId();

    memberService.deleteMember(userId);
    authService.logout(userId);

    return ResponseEntity.noContent().build();
  }
}