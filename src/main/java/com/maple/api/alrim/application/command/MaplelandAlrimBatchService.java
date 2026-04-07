package com.maple.api.alrim.application.command;

import com.maple.api.alrim.domain.Alrim;
import com.maple.api.alrim.domain.AlrimType;
import com.maple.api.alrim.repository.AlrimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaplelandAlrimBatchService {
  private final AlrimRepository alrimRepository;

  @Transactional(readOnly = true)
  public List<Alrim> excludeExistingFromCrawledAlrims(AlrimType alrimType, List<Alrim> crawledAlrims) {
    if (crawledAlrims.isEmpty()) {
      return List.of();
    }

    Set<String> crawledLinks = extractLinks(crawledAlrims);
    Set<String> existingLinks = findExistingLinks(alrimType, crawledLinks);

    return exculdeExisting(crawledAlrims, existingLinks);
  }

  private Set<String> extractLinks(List<Alrim> alrims) {
    return alrims.stream()
            .map(Alrim::getLink)
            .collect(Collectors.toSet());
  }

  private Set<String> findExistingLinks(AlrimType alrimType, Set<String> crawledLinks) {
    return alrimRepository.findAllByTypeAndLinkIn(alrimType, crawledLinks)
            .stream()
            .map(Alrim::getLink)
            .collect(Collectors.toSet());
  }

  private List<Alrim> exculdeExisting(List<Alrim> crawledAlrims, Set<String> existingLinks) {
    return crawledAlrims.stream()
            .filter(alrim -> !existingLinks.contains(alrim.getLink()))
            .toList();
  }

  @Transactional
  public void syncEventOutdatedStatusFromCrawl(List<Alrim> crawledEvents) {
    Map<String, Boolean> crawledOutdatedByLink = mapOutdatedByLink(crawledEvents);

    synchronizeExistingEventOutdated(crawledOutdatedByLink);
    markMissingOngoingEventsAsOutdated(crawledOutdatedByLink);
  }

  private Map<String, Boolean> mapOutdatedByLink(List<Alrim> crawledEvents) {
    return crawledEvents.stream()
      .collect(Collectors.toMap(Alrim::getLink, Alrim::getOutdated, (left, right) -> right));
  }

  private void synchronizeExistingEventOutdated(Map<String, Boolean> crawledOutdatedByLink) {
    alrimRepository.findAllByTypeAndLinkIn(AlrimType.EVENT, crawledOutdatedByLink.keySet())
      .forEach(savedEvent -> updateOutdatedStatusIfChanged(savedEvent, crawledOutdatedByLink));
  }

  private void updateOutdatedStatusIfChanged(Alrim savedEvent, Map<String, Boolean> crawledOutdatedByLink) {
    Boolean crawledOutdated = crawledOutdatedByLink.get(savedEvent.getLink());
    if (crawledOutdated == null || savedEvent.getOutdated().equals(crawledOutdated)) {
      return;
    }

    savedEvent.setOutdated(crawledOutdated);
    alrimRepository.save(savedEvent);
  }

  private void markMissingOngoingEventsAsOutdated(Map<String, Boolean> crawledOutdatedByLink) {
    alrimRepository.findAllByOutdatedIsFalseAndTypeOrderByDateDesc(AlrimType.EVENT)
      .stream()
      .filter(savedEvent -> isMissingFromCrawledEvents(savedEvent, crawledOutdatedByLink))
      .forEach(this::markAsOutdated);
  }

  private boolean isMissingFromCrawledEvents(Alrim savedEvent, Map<String, Boolean> crawledOutdatedByLink) {
    return !crawledOutdatedByLink.containsKey(savedEvent.getLink());
  }

  private void markAsOutdated(Alrim alrim) {
    alrim.setOutdated(true);
    alrimRepository.save(alrim);
  }
}
