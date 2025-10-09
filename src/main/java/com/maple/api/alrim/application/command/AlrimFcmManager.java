package com.maple.api.alrim.application.command;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.maple.api.alrim.domain.Alrim;
import com.maple.api.auth.repository.MemberRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlrimFcmManager {
  private final MemberRepository memberRepository;

  @PostConstruct
  public void init() {
    try (InputStream serviceAccount = getClass().getClassLoader()
      .getResourceAsStream("firebase/maple-9f1a7-firebase-adminsdk-fbsvc-7c3b6fc032.json")) {

      FirebaseOptions options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .build();

      if (FirebaseApp.getApps().isEmpty()) {
        FirebaseApp.initializeApp(options);
      }

    } catch (Exception e) {
      throw new IllegalStateException("Firebase 초기화 실패", e);
    }
  }

  public void sendFcmMessage(Alrim alrim) {
    memberRepository.findAllByFcmTokenIsNotNull()
      .stream()
      .filter(it -> switch (alrim.getType()) {
          case NOTICE -> it.getNoticeAgreement() != null && it.getNoticeAgreement();
          case PATCH_NOTE -> it.getPatchNoteAgreement() != null && it.getPatchNoteAgreement();
          case EVENT -> it.getEventAgreement() != null && it.getEventAgreement();
        }
      )
      .forEach(it -> {
        try {
          sendMessageDirect(
            it.getFcmToken(),
            switch (alrim.getType()) {
              case NOTICE -> "새 공지사항이 올라왔어요. 지금 확인해보세요!";
              case PATCH_NOTE -> "업데이트가 적용되었어요. 어떤 변화가 있었는지 확인해보세요!";
              case EVENT -> "새로운 이벤트가 시작되었습니다! 놓치지 마세요!";
            },
            alrim.getTitle()
          );

          log.info("{} 푸시 메세지 발송 성공 FCM 토큰 {} 유저 ID {}",
            alrim.getType(), it.getFcmToken(), it.getId());
        } catch (Exception e) {
          log.warn("{} 푸시 메세지 발송 실패 FCM 토큰 {} 유저 ID {}",
            alrim.getType(), it.getFcmToken(), it.getId());
        }
      });
  }

  // 원칙상은 private 인것이 좋겠지만 spy test 를 위해서 package-private 처리
  void sendMessageDirect(String targetToken, String title, String body) throws Exception {
    Message message = Message.builder()
      .setToken(targetToken)
      .setNotification(Notification.builder()
        .setTitle(title)
        .setBody(body)
        .build())
      .build();

    FirebaseMessaging.getInstance().send(message);
  }
}
