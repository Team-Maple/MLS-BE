package com.maple.api.auth.application;

import com.maple.api.auth.application.dto.LoginResponseDto;
import com.maple.api.auth.application.dto.MemberDto;
import com.maple.api.auth.repository.RefreshTokenRepository;
import com.maple.api.common.presentation.config.JwtTokenProvider;
import com.maple.api.common.presentation.config.JwtTokenValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  public Optional<LoginResponseDto> reissue(String refreshToken, MemberDto member) {
    if (!jwtTokenValidator.validateToken(refreshToken)) {
      return Optional.empty();
    }

    String userId = jwtTokenValidator.getUserDetails(refreshToken).getUsername();

    // 저장된 RefreshToken과 비교 (DB 또는 Redis)
    // JIN => 반영하지 않겠습니다. 여러 기기에서 로그인 되어야 하기 때문입니다.

//    String savedToken = refreshTokenService.get(userId);
//    if (!refreshToken.equals(savedToken)) {
//      return Optional.empty();
//    }

    String newAccessToken = jwtTokenProvider.createAccessToken(userId);
    String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

    // refreshToken 갱신 (보안 상 권장)
    refreshTokenService.save(userId, newRefreshToken);

    return Optional.of(new LoginResponseDto(newAccessToken, newRefreshToken, member));
  }
}
