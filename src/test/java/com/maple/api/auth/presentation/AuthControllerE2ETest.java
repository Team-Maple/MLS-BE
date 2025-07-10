package com.maple.api.auth.presentation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maple.api.auth.application.KakaoUserInfoClient;
import com.maple.api.auth.application.dto.CreateMemberRequestDto;
import com.maple.api.auth.application.dto.KakaoUserInfo;
import com.maple.api.auth.application.dto.LoginResponseDto;
import com.maple.api.auth.domain.Member;
import com.maple.api.auth.domain.Provider;
import com.maple.api.auth.repository.MemberRepository;
import com.maple.api.common.presentation.restapi.ResponseTemplate;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
          Provider.KAKAO,
          "NICKNAME",
          "",
          false
        )));
    }


    @Test
    @DisplayName("성공: 회원가입: 일반적인 닉네임")
    void signupSuccess() throws Exception {
      // when
      KakaoUserInfo kakaoUserInfo = createNonExistingKakaoUserInfo();
      when(kakaoUserInfoClient.getUserInfo(anyString())).thenReturn(kakaoUserInfo);
      when(memberRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

      CreateMemberRequestDto requestDto = new CreateMemberRequestDto(
        Provider.KAKAO,
        kakaoUserInfo.getId().toString(),
        "테스트닉네임",
        "fcm-token-1234", // FCM 토큰은 테스트용으로 임의로 설정,
        false
      );

      // then
      MvcResult result = mockMvc.perform(post("/api/v1/auth/signup/kakao")
          .header("access-token", "valid-access-token")
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON)
          .content((new ObjectMapper()).writeValueAsString(requestDto))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").exists())
        .andExpect(jsonPath("$.data.refreshToken").exists())
        .andReturn();

      System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("성공: 회원가입: 닉네임 없어도 성공")
    void signupSuccessWithoutNickname() throws Exception {
      // when
      KakaoUserInfo kakaoUserInfo = createNonExistingKakaoUserInfo();
      when(kakaoUserInfoClient.getUserInfo(anyString())).thenReturn(kakaoUserInfo);
      when(memberRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

      CreateMemberRequestDto requestDto = new CreateMemberRequestDto(
        Provider.KAKAO,
        kakaoUserInfo.getId().toString(),
        null,
        "fcm-token-1234", // FCM 토큰은 테스트용으로 임의로 설정
        false
      );

      // then
      MvcResult result = mockMvc.perform(post("/api/v1/auth/signup/kakao")
          .header("access-token", "valid-access-token")
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON)
          .content((new ObjectMapper()).writeValueAsString(requestDto))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").exists())
        .andExpect(jsonPath("$.data.refreshToken").exists())
        .andReturn();

      System.out.println(result.getResponse().getContentAsString());
    }


    @Test
    @DisplayName("성공: 가입된 사용자일 때 JWT 토큰 반환")
    void loginSuccess() throws Exception {
      // when
      KakaoUserInfo kakaoUserInfo = createExistingKakaoUserInfo();
      when(kakaoUserInfoClient.getUserInfo(anyString())).thenReturn(kakaoUserInfo);

      // then
      MvcResult result = mockMvc.perform(post("/api/v1/auth/login/kakao")
          .header("access-token", "valid-access-token")
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").exists())
        .andExpect(jsonPath("$.data.refreshToken").exists())
        .andReturn();

      System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("실패: Kakao 사용자 정보 조회 실패 시 401 반환")
    void loginFailInvalidKakaoUser() throws Exception {
      // when
      when(kakaoUserInfoClient.getUserInfo(anyString())).thenReturn(null);

      // then
      mockMvc.perform(post("/api/v1/auth/login/kakao")
          .header("access-token", "invalid-token"))
        .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("실패: 가입된 사용자가 없으면 404 반환")
      // 원래는 404 를 쓰는게 애매한 것이 맞습니다.
    void loginFailNoUser() throws Exception {
      // when
      KakaoUserInfo kakaoUserInfo = createNonExistingKakaoUserInfo();
      when(kakaoUserInfoClient.getUserInfo(anyString())).thenReturn(kakaoUserInfo);

      // then
      mockMvc.perform(post("/api/v1/auth/login/kakao")
          .header("access-token", "valid-token"))
        .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("회원 탈퇴 성공: 가입된 사용자가 아닐 때 탈퇴 성공")
    void 탈퇴성공() throws Exception {
      // given: 기존 회원 정보 및 kakaoUserInfoStub 준비
      KakaoUserInfo kakaoUserInfo = createExistingKakaoUserInfo();
      when(kakaoUserInfoClient.getUserInfo(anyString())).thenReturn(kakaoUserInfo);
      
      // when: 로그인 요청 후 토큰 발급
      String loginResponseBody = mockMvc.perform(post("/api/v1/auth/login/kakao")
          .header("access-token", "valid-access-token")
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").exists())
        .andExpect(jsonPath("$.data.refreshToken").exists())
        .andReturn()
        .getResponse()
        .getContentAsString();

      LoginResponseDto responseDTO = new ObjectMapper().readValue(
        loginResponseBody,
        new TypeReference<ResponseTemplate<LoginResponseDto>>() {
        }
      ).getData();

      String authorizationToken = responseDTO.accessToken();

      // then: 회원 탈퇴 요청
      mockMvc.perform(delete("/api/v1/auth/member")
          .header("Authorization", "Bearer " + authorizationToken)
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

      // verify: 회원 정보 삭제 확인
      verify(memberRepository).deleteById(kakaoUserInfo.getId().toString());
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