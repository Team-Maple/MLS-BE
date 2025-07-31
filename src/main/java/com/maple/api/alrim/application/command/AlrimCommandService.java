package com.maple.api.alrim.application.command;

import com.maple.api.alrim.domain.AlrimRead;
import com.maple.api.alrim.repository.AlrimReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlrimCommandService {
  private final AlrimReadRepository alrimReadRepository;

  public void readAlrim(
    String memberId,
    String alrimLink
  ) {
    val alrimRead = new AlrimRead(
      memberId,
      alrimLink
    );

    alrimReadRepository.save(alrimRead);
  }
}
