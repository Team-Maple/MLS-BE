package com.maple.api.alrim.application.command;

import com.maple.api.alrim.domain.Alrim;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component("legacyAlrimCrawler")
public class LegacyAlrimCrawler implements AlrimCrawler {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
  private static final String BASE_URL = "https://public.maple.land";

  @Override
  public List<Alrim> crawlNotices() throws IOException {
    String url = BASE_URL + "/notice";
    Document doc = Jsoup.connect(url)
      .userAgent("Mozilla/5.0")
      .get();

    return doc.select("div.notion-collection-item")
      .stream().map(el -> {
        String link = el.selectFirst("a").attr("href");
        String title = el.selectFirst("a").selectFirst("div:nth-of-type(1) > div:nth-of-type(2)").text();
        String date = el.selectFirst("a").selectFirst("div:nth-of-type(2) > div").text();

        return Alrim.createNotice(
          title,
          LocalDateTime.parse(date, FORMATTER).plusHours(9),
          BASE_URL + link
        );
      })
      .filter(it -> it.getTitle().length() >= 10)
      .toList();
  }

  @Override
  public List<Alrim> crawlPatchNotes() throws IOException {
    String url = BASE_URL + "/update";
    Document doc = Jsoup.connect(url)
      .userAgent("Mozilla/5.0")
      .get();

    return doc.select("div.notion-collection-item")
      .stream().map(el -> {
        String link = el.selectFirst("a").attr("href");
        String title = el.selectFirst("a").selectFirst("div:nth-of-type(1) > div:nth-of-type(2)").text();
        String date = el.selectFirst("a").selectFirst("div:nth-of-type(2) > div").text();

        return Alrim.createPatchNote(
          title,
          LocalDateTime.parse(date, FORMATTER).plusHours(9),
          BASE_URL + link
        );
      })
      .filter(it -> it.getTitle().length() >= 10)
      .toList();
  }

  @Override
  public List<Alrim> crawlEvents() throws IOException {
    String url = BASE_URL + "/events";
    Document doc = Jsoup.connect(url)
      .userAgent("Mozilla/5.0")
      .get();

    return doc.select("div.notion-collection-item")
      .stream().map(el -> {
        String link = el.selectFirst("a").attr("href");
        String title = el.selectFirst("a").selectFirst("div:nth-of-type(1) > div:nth-of-type(2)").text();
        String date = el.selectFirst("a").selectFirst("div:nth-of-type(2) > div").text();

        return Alrim.createEvents(
          title,
          LocalDateTime.parse(date, FORMATTER).plusHours(9),
          BASE_URL + link
        );
      })
      .filter(it -> it.getTitle().length() >= 10)
      .toList();
  }
}
