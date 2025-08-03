package com.maple.api.scheduler;

import com.maple.api.alrim.application.command.AlrimCrawler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class NoticeApiTest {

  @Test
  public void 공지사항테스트() throws Exception {
    AlrimCrawler crawler = new AlrimCrawler();
    var result = crawler.crawlNotices();

    assertThat(result.size()).isGreaterThan(0); // 명시적으로 길이 확인
  }

  @Test
  public void 패치노트테스트() throws Exception {
    AlrimCrawler crawler = new AlrimCrawler();
    var result = crawler.crawlPatchNotes();

    assertThat(result.size()).isGreaterThan(0); // 명시적으로 길이 확인
  }

  @Test
  public void 진행중이벤트테스트() throws Exception {
    AlrimCrawler crawler = new AlrimCrawler();
    var result = crawler.crawlEvents();

    assertThat(result.size()).isGreaterThan(0); // 명시적으로 길이 확인
  }
}
