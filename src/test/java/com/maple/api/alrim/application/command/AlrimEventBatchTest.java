package com.maple.api.alrim.application.command;

import com.maple.api.alrim.domain.Alrim;
import com.maple.api.alrim.domain.AlrimType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
  private AlrimCrawler crawler;

  @Mock
  private AlrimFcmManager alrimFcmManager;

  @InjectMocks
  private AlrimEventBatch batch;

  @Test()
  @DisplayName("batch 테스트")
  void batchTest() throws Exception {
    // given
    LocalDateTime now = LocalDateTime.now();

    // 공지
    given(commandBatchMediator.getRecentCreatedAt(AlrimType.NOTICE)).willReturn(now.minusDays(1));
    given(crawler.crawlNotices()).willReturn(List.of(
      Alrim.createNotice("제목 공지사항", now, "링크 공지사항")
    ));

    // 패치노트
    given(commandBatchMediator.getRecentCreatedAt(AlrimType.PATCH_NOTE)).willReturn(now.minusDays(1));
    given(crawler.crawlPatchNotes()).willReturn(List.of(
      Alrim.createPatchNote("패치노트 제목", now, "링크 패치노트")
    ));


    // 이벤트
    given(commandBatchMediator.getRecentCreatedAt(AlrimType.EVENT)).willReturn(now.minusDays(1));
    given(crawler.crawlEvents()).willReturn(List.of(
      Alrim.createEvents("이벤트", now, "링크 이벤트")
    ));

    // when
    batch.runCollectBatch();

    // then
    verify(commandBatchMediator, times(3)).saveAll(anyList());
    verify(alrimFcmManager).sendFcmMessage(any());
  }
}

