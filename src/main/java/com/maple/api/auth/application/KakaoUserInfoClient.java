package com.maple.api.auth.application;

import com.maple.api.auth.application.dto.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoUserInfoClient {
  private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
  private final RestTemplate restTemplate = new RestTemplate();

  public KakaoUserInfo getUserInfo(String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    HttpEntity<Void> request = new HttpEntity<>(headers);

    try {
      ResponseEntity<KakaoUserInfo> response = restTemplate.exchange(
        KAKAO_USER_INFO_URL,
        HttpMethod.GET,
        request,
        KakaoUserInfo.class
      );

      return response.getBody();
    } catch (Exception e) {
      log.error("카카오 사용자 정보 조회 실패", e);
      return null;
    }
  }
}
