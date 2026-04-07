package com.maple.api.alrim.application.command;

import com.maple.api.alrim.domain.Alrim;
import com.maple.api.alrim.domain.AlrimType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.alrim-event.enabled", havingValue = "true")
public class AlrimEventBatch {
  private final AlrimCommandMediator commandBatchMediator;
  private final MaplelandAlrimBatchService mapleLandAlrimBatchService;
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
      val notices = crawler.crawlNotices();
      val noticesWithoutExisting = mapleLandAlrimBatchService.excludeExistingFromCrawledAlrims(AlrimType.NOTICE, notices);
      commandBatchMediator.saveAll(noticesWithoutExisting);
      fcmCandidates.addAll(noticesWithoutExisting);

      log.info("공지사항 배치 저장 완료 {}", noticesWithoutExisting.size());
    } catch (Exception e) {
      log.error("공지사항 배치 저장 실패", e);
    }

    // STEP 02. 패치노트
    try {
      val patchNotes = crawler.crawlPatchNotes();
      val patchNotesWithoutExisting = mapleLandAlrimBatchService.excludeExistingFromCrawledAlrims(AlrimType.PATCH_NOTE, patchNotes);
      commandBatchMediator.saveAll(patchNotesWithoutExisting);
      fcmCandidates.addAll(patchNotesWithoutExisting);

      log.info("패치노트 배치 저장 완료 {}", patchNotesWithoutExisting.size());
    } catch (Exception e) {
      log.error("패치노트 배치 저장 실패", e);
    }

    // STEP 03. 새로운 이벤트 처리
    try {
      val events = crawler.crawlEvents();
      val eventsWithoutExisting = mapleLandAlrimBatchService.excludeExistingFromCrawledAlrims(AlrimType.EVENT, events);
      commandBatchMediator.saveAll(eventsWithoutExisting);
      fcmCandidates.addAll(eventsWithoutExisting.stream().filter(it -> !it.getOutdated()).toList());

      // 종료 상태 반영 및 누락된 진행중 이벤트 종료 처리
      mapleLandAlrimBatchService.syncEventOutdatedStatusFromCrawl(events);

      log.info("이벤트 배치 저장 완료 {}", eventsWithoutExisting.size());
    } catch (Exception e) {
      log.error("이벤트 배치 저장 실패", e);
    }

    // STEP 05. Fcm 전송 (FCM 은 도메인 이벤트보다는 어플리케이션 이벤트 레벨로 처리)
    if (!fcmCandidates.isEmpty()) {
      alrimFcmManager.sendFcmMessage(fcmCandidates.getFirst());
    }
  }
}
