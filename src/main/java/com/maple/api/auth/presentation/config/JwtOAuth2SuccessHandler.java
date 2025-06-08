package com.maple.api.auth.presentation.config;

import com.maple.api.auth.domain.PrincipalDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtOAuth2SuccessHandler implements AuthenticationSuccessHandler {

  private final JwtTokenProvider jwtTokenProvider;

  // 프론트엔드에서 처리할 redirect URI
//  private static final String REDIRECT_URI = "http://localhost:3000/oauth/callback";
  private static final String REDIRECT_URI = "myapp://login";

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
    throws IOException, ServletException {

    PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();

    String userId = principalDetails.getMember().getId();

    String accessToken = jwtTokenProvider.createAccessToken(userId);
    String refreshToken = jwtTokenProvider.createRefreshToken(userId);

    log.info("✅ OAuth2 로그인 성공 - userId: {}", userId);
    log.debug("accessToken: {}", accessToken);
    log.debug("refreshToken: {}", refreshToken);

    // 1. Refresh 토큰 저장 (DB 또는 Redis 등 - 생략 가능)

    // 2. Redirect 방식으로 토큰 전달 (예시: 쿼리스트링 사용)
    String targetUrl = REDIRECT_URI + "?access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
      + "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

    response.sendRedirect(targetUrl);
  }
}
