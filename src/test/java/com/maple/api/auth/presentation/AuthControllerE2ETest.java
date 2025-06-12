package com.maple.api.auth.presentation;

import com.maple.api.auth.application.KakaoUserInfoClient;
import com.maple.api.auth.application.dto.KakaoUserInfo;
import com.maple.api.auth.domain.Member;
import com.maple.api.auth.domain.Provider;
import com.maple.api.auth.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthControllerE2ETest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private KakaoUserInfoClient kakaoUserInfoClient;

  @MockitoBean
  private MemberRepository memberRepository;

  // MemberService와 AuthService는 실제 빈으로 동작

  @Nested
  @DisplayName("POST /api/v1/auth/login/kakao")
  class LoginWithKakao {
    // given
    KakaoUserInfo kakaoUserInfo;
    @BeforeEach
    void 초기유저한명_셋업() {
      kakaoUserInfo = createExistingKakaoUserInfo();
      // MemberRepository.findById 모킹
      when(memberRepository.findById(kakaoUserInfo.getId().toString()))
        .thenReturn(Optional.of(new Member(
          kakaoUserInfo.getId().toString(),
          "test@gmail.com",
          Provider.KAKAO
        )));
    }

    @Test
    @DisplayName("성공: 가입된 사용자일 때 JWT 토큰 반환")
    void loginSuccess() throws Exception {
      // when
      KakaoUserInfo kakaoUserInfo = createExistingKakaoUserInfo();
      when(kakaoUserInfoClient.getUserInfo(anyString())).thenReturn(kakaoUserInfo);

      // then
      mockMvc.perform(post("/api/v1/auth/login/kakao")
          .param("access_token", "valid-access-token")
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists())
        .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @DisplayName("실패: Kakao 사용자 정보 조회 실패 시 401 반환")
    void loginFailInvalidKakaoUser() throws Exception {
      // when
      when(kakaoUserInfoClient.getUserInfo(anyString())).thenReturn(null);

      // then
      mockMvc.perform(post("/api/v1/auth/login/kakao")
          .param("access_token", "invalid-token"))
        .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("실패: 가입된 사용자가 없으면 401 반환")
    void loginFailNoUser() throws Exception {
      // when
      KakaoUserInfo kakaoUserInfo = createNonExistingKakaoUserInfo();
      when(kakaoUserInfoClient.getUserInfo(anyString())).thenReturn(kakaoUserInfo);

      // then
      mockMvc.perform(post("/api/v1/auth/login/kakao")
          .param("access_token", "valid-token"))
        .andExpect(status().isUnauthorized());
    }
  }

  private KakaoUserInfo createExistingKakaoUserInfo() {
    return new KakaoUserInfo(
      12345L,
      new KakaoUserInfo.KakaoAccount(
        "test@gmail.com",
        new KakaoUserInfo.KakaoAccount.Profile(
          "Test User",
          "https://example.com/profile.jpg"
        )
      )
    );
  }

  private KakaoUserInfo createNonExistingKakaoUserInfo() {
    return new KakaoUserInfo(
      999999L,
      new KakaoUserInfo.KakaoAccount(
        "nontest@gmail.com",
        new KakaoUserInfo.KakaoAccount.Profile(
          "Test User",
          "https://example.com/profile.jpg"
        )
      )
    );
  }
}