package com.maple.api.alrim.application.command;

import com.maple.api.alrim.domain.Alrim;
import com.maple.api.alrim.domain.AlrimType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlrimEventBatchTest {

  @Mock
  private AlrimCommandMediator commandBatchMediator;

  @Mock
  private MaplelandAlrimBatchService mapleLandAlrimBatchService;

  @Mock
  private AlrimCrawler crawler;

  @Mock
  private AlrimFcmManager alrimFcmManager;

  @InjectMocks
  private AlrimEventBatch batch;

  @Test()
  @DisplayName("batch 테스트")
  void batchTest() throws Exception {
    // given
    Alrim notice = Alrim.createNotice("제목 공지사항", java.time.LocalDateTime.now(), "링크 공지사항");
    Alrim patchNote = Alrim.createPatchNote("패치노트 제목", java.time.LocalDateTime.now(), "링크 패치노트");
    Alrim ongoingEvent = Alrim.createEvents("진행중 이벤트", java.time.LocalDateTime.now(), "링크 진행중 이벤트");
    Alrim endedEvent = Alrim.createEvents("종료 이벤트", java.time.LocalDateTime.now(), "링크 종료 이벤트");
    endedEvent.markOutdated(true);

    // 공지
    given(crawler.crawlNotices()).willReturn(List.of(notice));
    given(mapleLandAlrimBatchService.excludeExistingFromCrawledAlrims(AlrimType.NOTICE, List.of(notice))).willReturn(List.of(notice));

    // 패치노트
    given(crawler.crawlPatchNotes()).willReturn(List.of(patchNote));
    given(mapleLandAlrimBatchService.excludeExistingFromCrawledAlrims(AlrimType.PATCH_NOTE, List.of(patchNote))).willReturn(List.of(patchNote));


    // 이벤트
    given(crawler.crawlEvents()).willReturn(List.of(ongoingEvent, endedEvent));
    given(mapleLandAlrimBatchService.excludeExistingFromCrawledAlrims(AlrimType.EVENT, List.of(ongoingEvent, endedEvent)))
      .willReturn(List.of(ongoingEvent, endedEvent));

    // when
    batch.runCollectBatch();

    // then
    verify(commandBatchMediator, times(3)).saveAll(anyList());
    verify(mapleLandAlrimBatchService).syncEventOutdatedStatusFromCrawl(List.of(ongoingEvent, endedEvent));
    verify(alrimFcmManager).sendFcmMessage(any());
  }
}
