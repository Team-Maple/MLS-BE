package com.maple.api.alrim.presentation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maple.api.alrim.application.query.AlrimDTOWithReadInfo;
import com.maple.api.alrim.common.CursorPage;
import com.maple.api.alrim.domain.Alrim;
import com.maple.api.alrim.domain.AlrimRead;
import com.maple.api.alrim.domain.AlrimType;
import com.maple.api.alrim.repository.AlrimReadRepository;
import com.maple.api.alrim.repository.AlrimRepository;
import com.maple.api.auth.domain.PrincipalDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AlrimControllerE2ETest {
  @Autowired
  private AlrimRepository alrimRepository; // 테스트용으로 직접 주입

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper; // JSON 변환용

  @MockitoBean
  private AlrimReadRepository alrimReadRepository;

  @Nested
  @DisplayName("GET /api/v1/alrim/all")
  class CursorPaginationTest {

    @Test
    @DisplayName("성공: 커서 기반 알림 페이지네이션 조회 총 2번")
    void cursorPaginationSuccess() throws Exception {
      // given: Test Auth
      UserDetails LoginedUser = createTestPrincipal("TESTUSER");

      // given: 알림 100개 DB에 삽입
      AlrimType[] types = AlrimType.values(); // [PATCH_NOTE, EVENT, NOTICE]

      for (int i = 0; i < 100; i++) {
        AlrimType type = types[i % types.length]; // 0 → PATCH_NOTE, 1 → EVENT, 2 → NOTICE 반복
        insertTestAlrim(
          LocalDateTime.now().minusMinutes(i * 3),
          type.getLabel() + " 알림 제목 " + i,
          "내용 " + i,
          type
        );
      }
      int size = 5;

      // when: 요청 1
      MvcResult result = mockMvc.perform(get("/api/v1/alrim/all")
          .param("pageSize", String.valueOf(size))
          .with(user(LoginedUser))
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

      String json = result.getResponse().getContentAsString();

      CursorPage<AlrimDTOWithReadInfo> response = objectMapper.readValue(
        json,
        new TypeReference<CursorPage<AlrimDTOWithReadInfo>>() {
        }
      );

      // then: 기대값 검증 1
      List<AlrimDTOWithReadInfo> content = response.getContents();
      assertThat(content).isNotEmpty();
      assertThat(content.size()).isEqualTo(size);
      assertThat(response.getHasMore()).isTrue();

      LocalDateTime cursorDate = content.getLast().alrim().date();

      // when: 요청 2
      MvcResult resultTwo = mockMvc.perform(get("/api/v1/alrim/all")
          .with(user(LoginedUser))
          .param("pageSize", String.valueOf(size))
          .param("cursor", cursorDate.toString())
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

      String json2 = resultTwo.getResponse().getContentAsString();

      CursorPage<AlrimDTOWithReadInfo> response2 = objectMapper.readValue(
        json2,
        new TypeReference<CursorPage<AlrimDTOWithReadInfo>>() {
        }
      );

      // then: 기대값 검증 2
      assertThat(response2.getContents().getFirst().alrim().date()).isBefore(cursorDate);
    }

    @Test
    @DisplayName("성공: GET /api/v1/alrim/read 알림 읽기를 했다면 반영하기")
    void cursorWithAlreadyReadTest() throws Exception {
      // given: Test Auth
      UserDetails LoginedUser = createTestPrincipal("TESTUSER");

      // given: 알림 5개 DB에 삽입
      AlrimType[] types = AlrimType.values(); // [PATCH_NOTE, EVENT, NOTICE]

      for (int i = 0; i < 5; i++) {
        AlrimType type = types[i % types.length]; // 0 → PATCH_NOTE, 1 → EVENT, 2 → NOTICE 반복
        insertTestAlrim(
          LocalDateTime.now().minusMinutes(i * 3),
          type.getLabel() + " 알림 제목 " + i,
          "내용 " + i,
          type
        );
      }
      int size = 5;

      // when
      when(alrimReadRepository.findAllByMemberIdAndAlrimLinkIn(any(), anyList()))
        .thenReturn(List.of(
          new AlrimRead(
            "내용 1",
            LoginedUser.getUsername()
          )
        ));

      // when: 요청 1
      MvcResult result = mockMvc.perform(get("/api/v1/alrim/all")
          .param("pageSize", String.valueOf(size))
          .with(user(LoginedUser))
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

      String json = result.getResponse().getContentAsString();

      CursorPage<AlrimDTOWithReadInfo> response = objectMapper.readValue(
        json,
        new TypeReference<CursorPage<AlrimDTOWithReadInfo>>() {
        }
      );

      // then: 기대값 검증 1
      List<AlrimDTOWithReadInfo> content = response.getContents();
      assertThat(content).isNotEmpty();
      assertThat(content.stream().filter(AlrimDTOWithReadInfo::alreadyRead).count()).isEqualTo(1);
      assertThat(content.size()).isEqualTo(size);

      // 1개 알림이 읽음 처리되어야 함
      assertThat(response.getHasMore()).isFalse();
    }


    private void insertTestAlrim(LocalDateTime dateTime, String title, String link, AlrimType type) {
      Alrim alrim = Alrim.builder()
        .title(title)
        .link(link)
        .date(dateTime)
        .type(type) // 예시용
        .build();

      alrimRepository.save(alrim);
    }

    private PrincipalDetails createTestPrincipal(String providerId) {
      return new PrincipalDetails(providerId);
    }
  }
}
