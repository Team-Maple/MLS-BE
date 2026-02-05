package com.maple.api.alrim.application.command;

import com.maple.api.alrim.domain.Alrim;
import com.maple.api.alrim.domain.AlrimType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlrimEventBatch {
  private final AlrimCommandMediator commandBatchMediator;
  private final AlrimCrawler crawler;
  private final AlrimFcmManager alrimFcmManager;

  // 서버 다중화시 락 필요
  // 매일 09:00~20:00 사이 0분에 실행
  @Scheduled(cron = "0 0 9-20 * * *", zone = "Asia/Seoul")
//  @Scheduled(initialDelay = 0, fixedRate = 60 * 1000 * 60) // 1시간마다 실행 (테스트용)
  public void runCollectBatch() {
    val fcmCandidates = new ArrayList<Alrim>();

    // STEP 01. 공지사항
    try {
      val notices = crawlNotices();
      commandBatchMediator.saveAll(notices);
      fcmCandidates.addAll(notices);

      log.info("공지사항 배치 저장 완료 {}", notices.size());
    } catch (Exception e) {
      log.error("공지사항 배치 저장 실패", e);
    }

    // STEP 02. 패치노트
    try {
      val patchNotes = crawlPatchNotes();
      commandBatchMediator.saveAll(patchNotes);
      fcmCandidates.addAll(patchNotes);

      log.info("패치노트 배치 저장 완료 {}", patchNotes.size());
    } catch (Exception e) {
      log.error("패치노트 배치 저장 실패", e);
    }

    // STEP 03. 새로운 이벤트 처리
    try {
      val events = crawlOnAllGoingEvents();
      val newEvents = newEventsForFCM(events);
      commandBatchMediator.saveAll(newEvents);
      fcmCandidates.addAll(newEvents);

      // 지난 이벤트는 Out Dated  처리
      commandBatchMediator.changeEventOutDatedExclude(events);

      log.info("진행중인 이벤트 배치 저장 완료 {}", newEvents.size());
    } catch (Exception e) {
      log.error("진행중인 이벤트 배치 저장 실패", e);
    }

    // STEP 05. Fcm 전송 (FCM 은 도메인 이벤트보다는 어플리케이션 이벤트 레벨로 처리)
    if (!fcmCandidates.isEmpty()) {
      alrimFcmManager.sendFcmMessage(fcmCandidates.getFirst());
    }
  }

  private List<Alrim> crawlNotices() throws IOException {
    var dateTime = commandBatchMediator.getRecentCreatedAt(AlrimType.NOTICE);
    return crawler.crawlNotices()
      .stream()
      .filter(it -> it.getDate().isAfter(dateTime))
      .toList();
  }

  private List<Alrim> crawlPatchNotes() throws IOException {
    var dateTime = commandBatchMediator.getRecentCreatedAt(AlrimType.PATCH_NOTE);
    return crawler.crawlPatchNotes()
      .stream()
      .filter(it -> it.getDate().isAfter(dateTime))
      .toList();
  }

  private List<Alrim> crawlOnAllGoingEvents() throws IOException {
    return crawler.crawlEvents();
  }

  private List<Alrim> newEventsForFCM(List<Alrim> alrimsList) throws IOException {
    var dateTime = commandBatchMediator.getRecentCreatedAt(AlrimType.EVENT);
    return alrimsList
      .stream()
      .filter(it -> it.getDate().isAfter(dateTime))
      .toList();
  }
}
