package com.maple.api.auth.presentation.restapi;

import com.maple.api.auth.application.AuthCommandService;
import com.maple.api.auth.application.RefreshTokenService;
import com.maple.api.auth.application.dto.TokenResponseDto;
import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.auth.presentation.config.JwtTokenProvider;
import com.maple.api.auth.presentation.config.JwtTokenValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtTokenValidator jwtTokenValidator;
  private final RefreshTokenService refreshTokenService;
  private final AuthCommandService authCommandService;

  @PostMapping("/reissue")
  public ResponseEntity<TokenResponseDto> reissue(@RequestHeader("Authorization") String refreshToken) {
    if (!jwtTokenValidator.validateToken(refreshToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String userId = jwtTokenValidator.getUserIdFromToken(refreshToken);

    // 저장된 RefreshToken과 비교 (DB 또는 Redis)
    String savedToken = refreshTokenService.get(userId);
    if (!refreshToken.equals(savedToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String newAccessToken = jwtTokenProvider.createAccessToken(userId);
    String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

    // refreshToken 갱신 (보안 상 권장)
    refreshTokenService.save(userId, newRefreshToken);

    return ResponseEntity.ok(new TokenResponseDto(newAccessToken, newRefreshToken));
  }


  @DeleteMapping("/me")
  public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal PrincipalDetails principalDetails) {
    String userId = principalDetails.getMember().getId();


    authCommandService.deleteMember(userId);
    refreshTokenService.delete(userId);

    return ResponseEntity.noContent().build();
  }
}