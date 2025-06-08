package com.maple.api.auth.presentation.restapi;

import com.maple.api.auth.application.AuthService;
import com.maple.api.auth.application.KakaoUserInfoClient;
import com.maple.api.auth.application.MemberService;
import com.maple.api.auth.application.dto.*;
import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.common.presentation.restapi.ResponseTemplate;
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

  @PostMapping("/login/apple")
  public ResponseEntity<ResponseTemplate<LoginResponseDto>> loginWithApple(
    @RequestParam("userId") String userId
  ) {
    // 가입된 유저인지 확인
    return memberService.findMember(userId)
      .map(member -> ResponseEntity.ok(
        ResponseTemplate.success(authService.login(member))
      ))
      .orElse(ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ResponseTemplate.failure("401", "가입되지 않은 사용자입니다.")));
  }

  @PostMapping("/login/kakao")
  public ResponseEntity<ResponseTemplate<LoginResponseDto>> loginWithKakao(@RequestParam("access_token") String accessToken) {
    KakaoUserInfo userInfo = kakaoUserInfoClient.getUserInfo(accessToken);
    if (userInfo == null || userInfo.getId() == null) {
      return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ResponseTemplate.failure("401", "카카오 로그인 access_token이 유효하지 않습니다. " + accessToken));
    }
    String kakaoId = userInfo.getId().toString();

    // 가입된 유저인지 확인
    return memberService.findMember(kakaoId)
      .map(member -> ResponseEntity.ok(
        ResponseTemplate.success(authService.login(member))
      ))
      .orElse(ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ResponseTemplate.failure("401", "가입되지 않은 사용자입니다.")));
  }

  @PostMapping("/signup/kakao")
  public ResponseEntity<ResponseTemplate<LoginResponseDto>> signupWithKakao(
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
      return ResponseEntity.ok(
        ResponseTemplate.success(authService.login(member))
      );
    } else {
      // 그냥 로그인 성공처리
      return ResponseEntity.ok(
        ResponseTemplate.success(authService.login(memberDtoOptional.get()))
      );
    }
  }

  @PostMapping("/signup/apple")
  public ResponseEntity<ResponseTemplate<LoginResponseDto>> signupWithApple(
    @RequestParam("userId") String userId,
    @RequestBody CreateMemberRequestDto createMemberRequestDto
  ) {
    Optional<MemberDto> memberDtoOptional = memberService.findMember(userId);
    if (memberDtoOptional.isEmpty()) {
      MemberDto member = memberService.createMember(createMemberRequestDto);
      return ResponseEntity.ok(
        ResponseTemplate.success(authService.login(member))
      );
    } else {
      // 그냥 로그인 성공처리
      return ResponseEntity.ok(
        ResponseTemplate.success(authService.login(memberDtoOptional.get()))
      );
    }
  }


  @PostMapping("/logout")
  public ResponseEntity<ResponseTemplate<Void>> logout(@AuthenticationPrincipal PrincipalDetails principalDetails) {
    authService.logout(principalDetails.getMember().getId());
    return ResponseEntity.ok(ResponseTemplate.success(null));
  }


  @PostMapping("/reissue")
  public ResponseEntity<ResponseTemplate<TokenResponseDto>> reissue(@RequestHeader("refresh_token") String refreshToken) {
    return authService.reissue(refreshToken)
      .map(res ->
        ResponseEntity.ok(ResponseTemplate.success(res))
      )
      .orElse(ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ResponseTemplate.failure("401", "잘못된 토큰입니다..")));
  }

  @PutMapping("/member")
  public ResponseEntity<ResponseTemplate<Void>> updateMyInfo(
    @RequestBody UpdateMemberRequestDto request
  ) {
    memberService.updateMember(request);
    return ResponseEntity.ok(ResponseTemplate.success(null));
  }


  @DeleteMapping("/member")
  public ResponseEntity<ResponseTemplate<Void>> deleteMe(@AuthenticationPrincipal PrincipalDetails principalDetails) {
    String userId = principalDetails.getMember().getId();

    memberService.deleteMember(userId);
    authService.logout(userId);

    return ResponseEntity.ok(ResponseTemplate.success(null));
  }
}