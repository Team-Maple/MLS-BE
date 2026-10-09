package com.maple.api.alrim.application.command;

import com.maple.api.alrim.domain.Alrim;
import com.maple.api.auth.domain.Member;
import com.maple.api.auth.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlrimFcmManagerTest {

  @Mock
  private MemberRepository memberRepository;

  @Spy
  @InjectMocks
  private AlrimFcmManager fcmManager;

  @Test
  @DisplayName("기본 테스트 FCM 유저 한명 (공지사항 true), FCM 없는 유저 한명일떄 공지사항이 있다면")
  void sendFcmMessage_noticeType_sendsToAgreedMembersOnly() throws Exception {
    // given
    var alrim = Alrim.createNotice("공지 제목", LocalDateTime.now(), "링크");

    var fcmMember = Member.builder()
      .id("user1")
      .fcmToken("token-1")
      .noticeAgreement(true)
      .patchNoteAgreement(false)
      .eventAgreement(false)
      .build();

    var nonFcmMember = Member.builder()
      .id("user2")
      .fcmToken(null)
      .build();

    given(memberRepository.findAllByFcmTokenIsNotNull())
      .willReturn(List.of(fcmMember));

    // when
    fcmManager.sendFcmMessage(alrim);

    // then
    verify(fcmManager, times(1)).sendMessageDirect(any(), any(), any());
  }

  @Nested
  @DisplayName("공지사항 테스트")
  class NoticesTest {
    @Test
    @DisplayName("공지사항은 2명에게 전달")
    void sendFcmTest() throws Exception {
      // given
      var alrim = Alrim.createNotice("공지 제목", LocalDateTime.now(), "링크");

      var fcmMember1 = Member.builder()
        .id("user1")
        .fcmToken("token-1")
        .noticeAgreement(true)
        .build();

      var fcmMember2 = Member.builder()
        .id("user2")
        .fcmToken(null)
        .noticeAgreement(true)
        .build();

      var fcmMemberFake = Member.builder()
        .id("user3")
        .fcmToken(null)
        .noticeAgreement(false)
        .eventAgreement(true)
        .build();

      given(memberRepository.findAllByFcmTokenIsNotNull())
        .willReturn(List.of(fcmMember1, fcmMember2, fcmMemberFake));

      // when
      fcmManager.sendFcmMessage(alrim);

      // then
      verify(fcmManager, times(2)).sendMessageDirect(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("패치노트 테스트")
  class PatchNotesTest {
    @Test
    @DisplayName("패치노트 는 2명에게 전달")
    void sendFcmTest() throws Exception {
      // given
      var alrim = Alrim.createPatchNote("패치노트 제목", LocalDateTime.now(), "링크");

      var fcmMember1 = Member.builder()
        .id("user1")
        .fcmToken("token-1")
        .patchNoteAgreement(true)
        .build();

      var fcmMember2 = Member.builder()
        .id("user2")
        .fcmToken(null)
        .patchNoteAgreement(true)
        .build();

      var fcmMemberFake = Member.builder()
        .id("user3")
        .fcmToken(null)
        .patchNoteAgreement(false)
        .eventAgreement(true)
        .build();

      given(memberRepository.findAllByFcmTokenIsNotNull())
        .willReturn(List.of(fcmMember1, fcmMember2, fcmMemberFake));

      // when
      fcmManager.sendFcmMessage(alrim);

      // then
      verify(fcmManager, times(2)).sendMessageDirect(any(), any(), any());
    }
  }


  @Nested
  @DisplayName("이벤트 테스트")
  class EventsTest {
    @Test
    @DisplayName("이벤트 는 2명에게 전달")
    void sendFcmTest() throws Exception {
      // given
      var alrim = Alrim.createEvents("이벤트 제목", LocalDateTime.now(), "링크");

      var fcmMember1 = Member.builder()
        .id("user1")
        .fcmToken("token-1")
        .eventAgreement(true)
        .build();

      var fcmMember2 = Member.builder()
        .id("user2")
        .fcmToken(null)
        .eventAgreement(true)
        .build();

      var fcmMemberFake = Member.builder()
        .id("user3")
        .fcmToken(null)
        .eventAgreement(false)
        .patchNoteAgreement(true)
        .build();

      given(memberRepository.findAllByFcmTokenIsNotNull())
        .willReturn(List.of(fcmMember1, fcmMember2, fcmMemberFake));

      // when
      fcmManager.sendFcmMessage(alrim);

      // then
      verify(fcmManager, times(2)).sendMessageDirect(any(), any(), any());
    }
  }
}

