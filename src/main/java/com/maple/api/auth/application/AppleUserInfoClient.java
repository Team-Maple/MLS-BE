package com.maple.api.auth.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AppleUserInfoClient {
  private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";
  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Apple ID Token을 검증하고 사용자 식별자(sub)를 반환합니다.
   *
   * @param idToken 클라이언트로부터 전달받은 Apple의 id_token
   * @return Apple 사용자 식별자 (sub) 또는 null
   */
  public String getUserIdFromIdToken(String idToken) {
    try {
      // kid 추출
      String kid = getKid(idToken);

      PublicKey publicKey = getApplePublicKeyByKid(kid);
      if (publicKey == null) {
        log.warn("Apple public key not found for kid: {}", kid);
        return null;
      }

      Claims claims = Jwts.parser()
        .verifyWith(publicKey)
        .build()
        .parseSignedClaims(idToken)
        .getPayload();

      return claims.getSubject(); // Apple 고유 사용자 ID
    } catch (Exception e) {
      log.error("Apple ID Token 검증 실패", e);
      return null;
    }
  }

  /**
   * Apple의 공개키 중 kid에 해당하는 RSA PublicKey를 반환
   */
  private PublicKey getApplePublicKeyByKid(String kid) throws Exception {
    Map<String, Object> response = restTemplate.getForObject(APPLE_KEYS_URL, Map.class);
    if (response == null || !response.containsKey("keys")) return null;

    List<Map<String, String>> keyList = (List<Map<String, String>>) response.get("keys");

    for (Map<String, String> key : keyList) {
      if (!kid.equals(key.get("kid"))) continue;

      byte[] modulusBytes = Base64.getUrlDecoder().decode(key.get("n"));
      byte[] exponentBytes = Base64.getUrlDecoder().decode(key.get("e"));

      BigInteger modulus = new BigInteger(1, modulusBytes);
      BigInteger exponent = new BigInteger(1, exponentBytes);

      RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(modulus, exponent);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePublic(publicKeySpec);
    }

    return null;
  }

  public String getKid(String idToken) {
    try {
      String[] parts = idToken.split("\\.");
      if (parts.length < 2) {
        return null;
      }
      String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));

      JsonNode rootNode = objectMapper.readTree(headerJson);
      JsonNode kidNode = rootNode.get("kid");
      return kidNode != null ? kidNode.asText() : null;
    } catch (Exception e) {
      log.error("Failed to parse kid from idToken header", e);
      return null;
    }
  }

}
