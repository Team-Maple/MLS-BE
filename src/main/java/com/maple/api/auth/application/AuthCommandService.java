package com.maple.api.auth.application;

import com.maple.api.auth.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthCommandService {
  private final MemberRepository memberRepository;

  public void deleteMember(String memberId) {
    log.info("Deleting member with memberId: {}", memberId);
    memberRepository.deleteById(memberId);
  }
}
