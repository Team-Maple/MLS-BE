package com.maple.api.common.presentation.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

  @Value("${jwt.secret}")
  private String secret;

//  private static final long ACCESS_TOKEN_EXPIRY = 1000L * 60 * 60;  // 1시간
  private static final long ACCESS_TOKEN_EXPIRY = 1000L * 60 * 60 * 24 * 365 * 10;  // 10년
//  private static final long REFRESH_TOKEN_EXPIRY = 1000L * 60 * 60 * 24 * 7; // 7일
  private static final long REFRESH_TOKEN_EXPIRY = 1000L * 60 * 60 * 24 * 365 * 10; // 10년

  public String createAccessToken(String userId) {
    return createToken(userId, ACCESS_TOKEN_EXPIRY);
  }

  public String createRefreshToken(String userId) {
    return createToken(userId, REFRESH_TOKEN_EXPIRY);
  }

  private String createToken(String userId, long expiry) {
    Date now = new Date();
    return Jwts.builder()
      .setSubject(userId)
      .setIssuedAt(now)
      .setExpiration(new Date(now.getTime() + expiry))
      .signWith(SignatureAlgorithm.HS256, Base64.getDecoder().decode(secret))
      .compact();
  }
}
