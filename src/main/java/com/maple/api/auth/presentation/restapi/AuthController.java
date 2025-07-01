package com.maple.api.auth.presentation.restapi;

import com.maple.api.auth.application.AppleUserInfoClient;
import com.maple.api.auth.application.AuthService;
import com.maple.api.auth.application.KakaoUserInfoClient;
import com.maple.api.auth.application.MemberService;
import com.maple.api.auth.application.dto.*;
import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.common.presentation.config.JwtTokenValidator;
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
  private final JwtTokenValidator jwtTokenValidator;
  private final KakaoUserInfoClient kakaoUserInfoClient;
  private final AppleUserInfoClient appleUserInfoClient;

  @PostMapping("/login/kakao")
  public ResponseEntity<ResponseTemplate<LoginResponseDto>> loginWithKakao(
    @RequestHeader("access-token") String accessToken
  ) {
    KakaoUserInfo userInfo = kakaoUserInfoClient.getUserInfo(accessToken);
    if (userInfo == null || userInfo.getId() == null) {
      return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ResponseTemplate.failure("401", "카카오 로그인 access_token이 유효하지 않습니다."));
    }
    String kakaoId = userInfo.getId().toString();

    // 가입된 유저인지 확인
    return memberService.findMember(kakaoId)
      .map(member -> ResponseEntity.ok(
        ResponseTemplate.success(authService.login(member))
      ))
      .orElse(ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ResponseTemplate.failure("404", "가입되지 않은 사용자입니다.")));
  }

  @PostMapping("/login/apple")
  public ResponseEntity<ResponseTemplate<LoginResponseDto>> loginWithApple(
    @RequestHeader("id-token") String idToken
  ) {
    String appleUserId = appleUserInfoClient.getUserIdFromIdToken(idToken);

    if (appleUserId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ResponseTemplate.failure("401", "Apple ID Token이 유효하지 않습니다."));
    }

    return memberService.findMember(appleUserId)
      .map(member -> ResponseEntity.ok(ResponseTemplate.success(authService.login(member))))
      .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ResponseTemplate.failure("404", "가입되지 않은 사용자입니다.")));
  }


  @PostMapping("/signup/kakao")
  public ResponseEntity<ResponseTemplate<LoginResponseDto>> signupWithKakao(
    @RequestHeader("access-token") String accessToken,
    @RequestBody CreateMemberRequestDto createMemberRequestDto
  ) {
    KakaoUserInfo userInfo = kakaoUserInfoClient.getUserInfo(accessToken);
    if (userInfo == null || userInfo.getId() == null) {
      return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ResponseTemplate.failure("401", "카카오 로그인 access_token이 유효하지 않습니다."));
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
    @RequestHeader("id-token") String idToken,
    @RequestBody CreateMemberRequestDto createMemberRequestDto
  ) {
    String appleUserId = appleUserInfoClient.getUserIdFromIdToken(idToken);
    if (appleUserId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ResponseTemplate.failure("401", "Apple ID Token이 유효하지 않습니다."));
    }

    Optional<MemberDto> memberDtoOptional = memberService.findMember(appleUserId);
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

  @PostMapping("/reissue")
  public ResponseEntity<ResponseTemplate<LoginResponseDto>> reissue(
    @RequestHeader("refresh-token") String refreshToken
  ) {
    String userId = jwtTokenValidator.getUserDetails(refreshToken).getUsername();
    if (userId == null) {
      return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ResponseTemplate.failure("401", "잘못된 토큰입니다."));
    }

    Optional<MemberDto> memberOpt = memberService.findMember(userId);

    return memberOpt.map(memberDto -> authService.reissue(refreshToken.replace("Bearer ", ""), memberDto)
      .map(res ->
        ResponseEntity.ok(ResponseTemplate.success(res))
      )
      .orElse(ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ResponseTemplate.failure("401", "잘못된 토큰입니다..")))).orElseGet(() -> ResponseEntity
      .status(HttpStatus.UNAUTHORIZED)
      .body(ResponseTemplate.failure("404", "가입된 유저가 없습니다.")));
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