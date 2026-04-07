package com.maple.api.alrim.application.command;

import com.maple.api.alrim.domain.Alrim;
import com.maple.api.alrim.domain.AlrimRead;
import com.maple.api.alrim.exception.AlrimException;
import com.maple.api.alrim.repository.AlrimReadRepository;
import com.maple.api.alrim.repository.AlrimRepository;
import com.maple.api.common.presentation.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlrimCommandService {
  private final AlrimReadRepository alrimReadRepository;
  private final AlrimRepository alrimRepository;

  @Transactional
  public void setReadAlrim(
    String memberId,
    String alrimLink
  ) {
    Alrim alrim = alrimRepository.findByLink(alrimLink)
      .orElseThrow(() -> ApiException.of(AlrimException.ALRIM_NOT_FOUND));

    if (alrimReadRepository.existsByMemberIdAndAlrimId(memberId, alrim.getId())) {
      return;
    }

    alrimReadRepository.save(new AlrimRead(alrim.getId(), alrimLink, memberId));
  }
}
