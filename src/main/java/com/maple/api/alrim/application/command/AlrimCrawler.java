package com.maple.api.alrim.application.command;

import com.maple.api.alrim.domain.Alrim;

import java.io.IOException;
import java.util.List;

public interface AlrimCrawler {
  List<Alrim> crawlNotices() throws IOException;

  List<Alrim> crawlPatchNotes() throws IOException;

  List<Alrim> crawlEvents() throws IOException;
}
