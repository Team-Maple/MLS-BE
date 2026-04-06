package com.maple.api.alrim.application.command;

import com.maple.api.alrim.domain.Alrim;
import com.maple.api.alrim.domain.AlrimType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Primary
@Component
public class MaplelandAlrimCrawler implements AlrimCrawler {
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
  private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}\\.\\d{2}\\.\\d{2}");
  private static final String BASE_URL = "https://maple.land";
  private static final String NOTICES_PATH = "/board/notices";
  private static final Pattern NOTICE_LINK_PATTERN = Pattern.compile("^/board/notices/[a-z0-9]+$");
  private static final String EVENTS_PATH = "/board/events";
  private static final Pattern EVENT_LINK_PATTERN = Pattern.compile("^/board/events/[a-z0-9]+$");

  @Override
  public List<Alrim> crawlNotices() throws IOException {
    return crawlAllPages(NOTICES_PATH, html ->
      parseNoticeBoard(html, category -> !"업데이트".equals(category), AlrimType.NOTICE)
    );
  }

  @Override
  public List<Alrim> crawlPatchNotes() throws IOException {
    return crawlAllPages(NOTICES_PATH, html ->
      parseNoticeBoard(html, "업데이트"::equals, AlrimType.PATCH_NOTE)
    );
  }

  @Override
  public List<Alrim> crawlEvents() throws IOException {
    return crawlAllPages(EVENTS_PATH, this::parseEvents);
  }

  List<Alrim> parseNotices(String html) {
    return parseNoticeBoard(html, category -> !"업데이트".equals(category), AlrimType.NOTICE);
  }

  List<Alrim> parsePatchNotes(String html) {
    return parseNoticeBoard(html, "업데이트"::equals, AlrimType.PATCH_NOTE);
  }

  List<Alrim> parseEvents(String html) {
    return parseEventBoard(html);
  }

  private List<Alrim> parseNoticeBoard(String html, Predicate<String> categoryFilter, AlrimType type) {
    Document doc = Jsoup.parse(html, BASE_URL);
    return doc.select("a[href]")
      .stream()
      .filter(anchor -> NOTICE_LINK_PATTERN.matcher(anchor.attr("href")).matches())
      .map(anchor -> toBoardRow(anchor, type))
      .filter(row -> row != null && categoryFilter.test(row.category()))
      .map(this::toAlrim)
      .toList();
  }

  private List<Alrim> parseEventBoard(String html) {
    Document doc = Jsoup.parse(html, BASE_URL);
    return doc.select("a[href]")
      .stream()
      .filter(anchor -> EVENT_LINK_PATTERN.matcher(anchor.attr("href")).matches())
      .map(anchor -> toBoardRow(anchor, AlrimType.EVENT))
      .filter(row -> row != null && isSupportedEventCategory(row.category()))
      .map(this::toAlrim)
      .toList();
  }

  private BoardRow toBoardRow(Element anchor, AlrimType type) {
    Element row = anchor.parent() != null ? anchor.parent().parent() : null;
    if (row == null) {
      return null;
    }

    Element categoryElement = row.selectFirst("span.inline-flex");
    String category = categoryElement != null ? categoryElement.text().trim() : "";
    String title = anchor.text().trim();
    String link = anchor.absUrl("href");

    var matcher = DATE_PATTERN.matcher(row.text());
    if (category.isBlank() || title.isBlank() || link.isBlank() || !matcher.find()) {
      return null;
    }

    LocalDateTime date = LocalDate.parse(matcher.group(), DATE_FORMATTER).atStartOfDay();
    return new BoardRow(type, category, title, date, link);
  }

  private Alrim toAlrim(BoardRow row) {
    Alrim alrim = switch (row.type()) {
      case NOTICE -> Alrim.createNotice(row.title(), row.date(), row.link());
      case PATCH_NOTE -> Alrim.createPatchNote(row.title(), row.date(), row.link());
      case EVENT -> Alrim.createEvents(row.title(), row.date(), row.link());
    };

    if (row.type() == AlrimType.EVENT) {
      alrim.markOutdated("종료".equals(row.category()));
    }

    return alrim;
  }

  private boolean isSupportedEventCategory(String category) {
    return "진행중".equals(category) || "종료".equals(category);
  }

  private List<Alrim> crawlAllPages(String path, Function<String, List<Alrim>> parser) throws IOException {
    String firstPageHtml = fetchBoardPage(path);
    List<Alrim> results = new ArrayList<>(parser.apply(firstPageHtml));

    int lastPage = extractLastPage(Jsoup.parse(firstPageHtml, BASE_URL));
    for (int page = 2; page <= lastPage; page++) {
      results.addAll(parser.apply(fetchBoardPage(path + "?page=" + page)));
    }

    return results;
  }

  private int extractLastPage(Document doc) {
    return doc.select("nav[aria-label=페이지 네비게이션] a[href*=page=]")
      .stream()
      .map(element -> element.attr("href"))
      .mapToInt(this::extractPageNumber)
      .max()
      .orElse(1);
  }

  private int extractPageNumber(String href) {
    int index = href.indexOf("page=");
    if (index < 0) {
      return 1;
    }

    String page = href.substring(index + 5).replaceAll("[^0-9].*$", "");
    return page.isBlank() ? 1 : Integer.parseInt(page);
  }

  private String fetchBoardPage(String path) throws IOException {
    return Jsoup.connect(BASE_URL + path)
      .userAgent("Mozilla/5.0")
      .ignoreContentType(true)
      .execute()
      .body();
  }

  private record BoardRow(
    AlrimType type,
    String category,
    String title,
    LocalDateTime date,
    String link
  ) {
  }
}
