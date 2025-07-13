package com.maple.api.auth.application;

import com.maple.api.auth.application.dto.CreateMemberRequestDto;
import com.maple.api.auth.application.dto.MemberDto;
import com.maple.api.auth.application.dto.UpdateCommand;
import com.maple.api.auth.domain.Member;
import com.maple.api.auth.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {
  private final MemberRepository memberRepository;

  @Transactional
  public Optional<MemberDto> findMember(String memberId) {
    return memberRepository.findById(memberId)
      .map(MemberDto::toDto);
  }

  @Transactional
  public MemberDto createMember(CreateMemberRequestDto createMemberRequestDto) {
    log.info("Create member with memberId: {}", createMemberRequestDto.getProviderId());
    Member result = memberRepository.save(new Member(
      createMemberRequestDto.getProviderId(),
      createMemberRequestDto.getProvider(),
      createMemberRequestDto.getNickname(),
      createMemberRequestDto.getFcmToken(),
      createMemberRequestDto.getMarketingAgreement()
    ));
    return MemberDto.toDto(result);
  }

  @Transactional
  public Optional<MemberDto> updateNickname(String memberId, String nickName) {
    log.info("Update member with memberId: {} / nickName: {}", memberId, nickName);

    return memberRepository.findById(memberId)
      .map(m -> {
        m.setNickname(nickName);
        return m;
      })
      .map(MemberDto::toDto);
  }

  @Transactional
  public Optional<MemberDto> updateMarketingAgreement(String memberId, Boolean marketingAgreement) {
    log.info("Update member with memberId: {} / marketingAgreement: {}", memberId, marketingAgreement);

    return memberRepository.findById(memberId)
      .map(m -> {
        m.setMarketingAgreement(marketingAgreement);
        return m;
      })
      .map(MemberDto::toDto);
  }

  @Transactional
  public Optional<MemberDto> updateFcmToken(String memberId, String fcmToken) {
    log.info("Update member with memberId: {} / fcmToken: {}", memberId, fcmToken);

    return memberRepository.findById(memberId)
      .map(m -> {
        m.setFcmToken(fcmToken);
        return m;
      })
      .map(MemberDto::toDto);
  }

  @Transactional
  public Optional<MemberDto> updateAlertAgreement(String memberId, UpdateCommand.Agreements updateAlertAgreement) {
    return memberRepository.findById(memberId)
      .map(m -> {
        m.setAlertAgreements(
          updateAlertAgreement.getNoticeAgreement(),
          updateAlertAgreement.getPatchNoteAgreement(),
          updateAlertAgreement.getEventAgreement()
        );
        return m;
      })
      .map(MemberDto::toDto);
  }

  @Transactional
  public void deleteMember(String memberId) {
    log.info("Deleting member with memberId: {}", memberId);
    memberRepository.deleteById(memberId);
  }
}
