package com.maple.api.auth.repository;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RefreshTokenRepository {

  private final Map<String, String> refreshTokenStore = new ConcurrentHashMap<>();

  /**
   * refreshToken 저장
   */
  public void save(String userId, String refreshToken) {
    refreshTokenStore.put(userId, refreshToken);
  }

  /**
   * refreshToken 조회
   */
  public String get(String userId) {
    return refreshTokenStore.get(userId);
  }

  /**
   * refreshToken 삭제
   */
  public void delete(String userId) {
    refreshTokenStore.remove(userId);
  }

  /**
   * 저장된 refreshToken과 비교하여 유효성 검증
   */
  public boolean isValid(String userId, String refreshToken) {
    return refreshToken.equals(refreshTokenStore.get(userId));
  }
}

