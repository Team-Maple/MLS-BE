package com.maple.api.auth.application;

import com.maple.api.auth.application.dto.KakaoUserInfo;
import com.maple.api.common.logging.SafeExceptionLog;
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
      SafeExceptionLog.addException(log.atError(), e)
        .addKeyValue("event.action", "external.user-info.fetch")
        .addKeyValue("event.outcome", "failure")
        .addKeyValue("mapleland.external.system", "kakao")
        .log("Kakao user information request failed");
      return null;
    }
  }
}
