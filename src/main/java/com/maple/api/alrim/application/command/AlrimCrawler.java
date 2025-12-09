package com.maple.api.alrim.application.command;

import com.maple.api.alrim.domain.Alrim;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class AlrimCrawler {
  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
  private final String BaseUrlString = "https://public.maple.land";

  public List<Alrim> crawlNotices() throws IOException {
    String url = "https://public.maple.land/notice";
    Document doc = Jsoup.connect(url)
      .userAgent("Mozilla/5.0")
      .get();

    return doc.select("div.notion-collection-item")
      .stream().map(el -> {
        String link = el.selectFirst("a").attr("href");

        String title = el.selectFirst("a").selectFirst("div:nth-of-type(1) > div:nth-of-type(2)").text();
        String date = el.selectFirst("a").selectFirst("div:nth-of-type(2) > div").text();

//        log.info("제목: {}, 날짜: {}, 링크: {}", title, date, link);
        return Alrim.createNotice(
          title,
          LocalDateTime.parse(date, formatter),
          BaseUrlString + link
        );
      })
      .filter(it -> it.getTitle().length() >= 10)
      .toList();
  }

  public List<Alrim> crawlPatchNotes() throws IOException {
    String url = "https://public.maple.land/update";
    Document doc = Jsoup.connect(url)
      .userAgent("Mozilla/5.0")
      .get();

    return doc.select("div.notion-collection-item")
      .stream().map(el -> {
        String link = el.selectFirst("a").attr("href");

        String title = el.selectFirst("a").selectFirst("div:nth-of-type(1) > div:nth-of-type(2)").text();
        String date = el.selectFirst("a").selectFirst("div:nth-of-type(2) > div").text();

//        log.info("제목: {}, 날짜: {}, 링크: {}", title, date, link);
        return Alrim.createPatchNote(
          title,
          LocalDateTime.parse(date, formatter),
          BaseUrlString + link
        );
      }).filter(it -> it.getTitle().length() >= 10).toList();
  }

  public List<Alrim> crawlEvents() throws IOException {
    String url = "https://public.maple.land/events";
    Document doc = Jsoup.connect(url)
      .userAgent("Mozilla/5.0")
      .get();

    return doc.select("div.notion-collection-item")
      .stream().map(el -> {
        String link = el.selectFirst("a").attr("href");

        String title = el.selectFirst("a").selectFirst("div:nth-of-type(1) > div:nth-of-type(2)").text();
        String date = el.selectFirst("a").selectFirst("div:nth-of-type(2) > div").text();

//        log.info("제목: {}, 날짜: {}, 링크: {}", title, date, link);
        return Alrim.createEvents(
          title,
          LocalDateTime.parse(date, formatter),
          BaseUrlString + link
        );
      }).filter(it -> it.getTitle().length() >= 10).toList();
  }
}
