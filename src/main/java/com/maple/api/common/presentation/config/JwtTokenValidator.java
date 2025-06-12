package com.maple.api.common.presentation.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class JwtTokenValidator {
  public static final String BEARER_PREFIX = "Bearer ";
  private Key key;

  public JwtTokenValidator(@Value("${jwt.secret}") String secretKey) {
    this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
  }

  public Boolean validateToken(String token) {
    return isValid(token, key);
  }


  public String resolveToken(HttpServletRequest request,
                             String tokenType) {
    String bearerToken = request.getHeader(tokenType);
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(7);
    } else if (StringUtils.hasText(bearerToken) && !bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken;
    }
    return null;
  }

  private Boolean isValid(String accessToken, Key key) {
    if (accessToken.contains(BEARER_PREFIX)) {
      accessToken = accessToken.replace(BEARER_PREFIX, "").trim();
    }
    try {
      Jwts.parser().verifyWith((SecretKey) key).build().parseSignedClaims(accessToken).getPayload();
      return true; // 유효하면 true 반환
    } catch (ExpiredJwtException e) {
      // 토큰이 만료된 경우
      return false;
    } catch (Exception e) {
      // 유효하지 않은 토큰인 경우
      return false;
    }
  }

  public UserDetails getUserDetails(String token) {
    try {
      Claims claims = Jwts.parser()
        .verifyWith((SecretKey) key)
        .build()
        .parseSignedClaims(token)
        .getPayload();

      Collection<? extends GrantedAuthority> grantedAuthorities = (Arrays.asList("ROLE_USER")).stream()
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());

      return new User(claims.getSubject(), "", grantedAuthorities);
    } catch (Exception e) {
      return null;
    }
  }
}
