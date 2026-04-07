package com.maple.api.scheduler;

import com.maple.api.alrim.application.command.AlrimCrawler;
import com.maple.api.alrim.application.command.MaplelandAlrimCrawler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class NoticeApiTest {

  @Test
  public void 공지사항테스트() throws Exception {
    AlrimCrawler crawler = new MaplelandAlrimCrawler();
    var result = crawler.crawlNotices();

    assertThat(result).isNotNull();
  }

  @Test
  public void 패치노트테스트() throws Exception {
    AlrimCrawler crawler = new MaplelandAlrimCrawler();
    var result = crawler.crawlPatchNotes();

    assertThat(result).isNotNull();
  }

  @Test
  public void 진행중이벤트테스트() throws Exception {
    AlrimCrawler crawler = new MaplelandAlrimCrawler();
    var result = crawler.crawlEvents();

    assertThat(result).isNotNull();
  }
}
