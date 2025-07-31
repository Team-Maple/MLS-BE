package com.maple.api.alrim.application.command;

import com.maple.api.alrim.domain.Alrim;
import com.maple.api.alrim.domain.AlrimType;
import com.maple.api.alrim.repository.AlrimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlrimCommandMediator {
  private final AlrimRepository alrimRepository;

  @Transactional
  public LocalDateTime getRecentCreatedAt(AlrimType alrimType) {
    return alrimRepository.findTopByTypeOrderByDateDesc(alrimType)
      .map(Alrim::getDate)
      .orElse(LocalDateTime.now().minusYears(5));
  }

  @Transactional
  public void changeEventOutDatedExclude(List<Alrim> newAlrimList) {
    val newAlrimLinkList = newAlrimList.stream()
      .map(Alrim::getLink)
      .collect(Collectors.toSet());

    alrimRepository.findAllByOutdatedIsFalseAndTypeOrderByDateDesc(AlrimType.EVENT)
      .stream()
      .filter(it -> !newAlrimLinkList.contains(it.getLink()))
      .forEach(it -> {
        it.setOutdated(true);
        alrimRepository.save(it);
      });
  }

  @Transactional
  public void saveAll(Iterable<Alrim> alrims) {
    alrimRepository.saveAll(alrims);
//    log.info("알림 저장 완료: {}", alrims);
  }
}
