package com.maple.api.auth.application;

import com.maple.api.auth.application.dto.LoginResponseDto;
import com.maple.api.auth.application.dto.MemberDto;
import com.maple.api.auth.application.dto.TokenResponseDto;
import com.maple.api.auth.presentation.config.JwtTokenProvider;
import com.maple.api.auth.presentation.config.JwtTokenValidator;
import com.maple.api.auth.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {
  private final RefreshTokenRepository refreshTokenService;
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtTokenValidator jwtTokenValidator;

  public LoginResponseDto login(MemberDto member) {
    String newAccessToken = jwtTokenProvider.createAccessToken(member.id());
    String newRefreshToken = jwtTokenProvider.createRefreshToken(member.id());

    refreshTokenService.save(member.id(), newRefreshToken);
    return new LoginResponseDto(newAccessToken, newRefreshToken, member);
  }

  public void logout(String memberId) {
    log.info("Logging out member with ID: {}", memberId);
    refreshTokenService.delete(memberId);
  }

  public Optional<TokenResponseDto> reissue(String refreshToken) {
    if (!jwtTokenValidator.validateToken(refreshToken)) {
      return Optional.empty();
    }

    String userId = jwtTokenValidator.getUserIdFromToken(refreshToken);

    // 저장된 RefreshToken과 비교 (DB 또는 Redis)
    String savedToken = refreshTokenService.get(userId);
    if (!refreshToken.equals(savedToken)) {
      return Optional.empty();
    }

    String newAccessToken = jwtTokenProvider.createAccessToken(userId);
    String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

    // refreshToken 갱신 (보안 상 권장)
    refreshTokenService.save(userId, newRefreshToken);

    return Optional.of(new TokenResponseDto(newAccessToken, newRefreshToken));
  }
}
