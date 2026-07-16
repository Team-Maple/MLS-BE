package com.maple.api.auth.application;

import com.maple.api.auth.application.dto.CreateMemberRequestDto;
import com.maple.api.auth.application.dto.MemberDto;
import com.maple.api.auth.application.dto.UpdateCommand;
import com.maple.api.auth.application.exception.AuthException;
import com.maple.api.auth.domain.Member;
import com.maple.api.auth.repository.MemberRepository;
import com.maple.api.common.presentation.exception.ApiException;
import com.maple.api.job.domain.Job;
import com.maple.api.job.exception.JobException;
import com.maple.api.job.repository.JobRepository;
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
  private final JobRepository jobRepository;

  @Transactional
  public Optional<MemberDto> findMember(String memberId) {
    return memberRepository.findById(memberId)
      .map(MemberDto::toDto);
  }

  @Transactional
  public MemberDto findMe(String memberId) {
    return memberRepository.findById(memberId)
      .map(MemberDto::toDto)
      .orElseThrow(() -> new ApiException(AuthException.NO_MEMBER));
  }

  @Transactional
  public MemberDto createMember(CreateMemberRequestDto createMemberRequestDto) {
    log.atInfo()
      .addKeyValue("event.action", "member.create")
      .log("Member creation requested");
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
  public MemberDto updateNickname(String memberId, String nickName) {
    log.atInfo()
      .addKeyValue("event.action", "member.nickname.update")
      .log("Member nickname update requested");

    return memberRepository.findById(memberId)
      .map(m -> {
        m.setNickname(nickName);
        return m;
      })
      .map(MemberDto::toDto)
      .orElseThrow(() -> new ApiException(AuthException.NO_MEMBER));
  }

  @Transactional
  public MemberDto updateMarketingAgreement(String memberId, Boolean marketingAgreement) {
    log.atInfo()
      .addKeyValue("event.action", "member.marketing-agreement.update")
      .log("Member marketing agreement update requested");

    return memberRepository.findById(memberId)
      .map(m -> {
        m.setMarketingAgreement(marketingAgreement);
        return m;
      })
      .map(MemberDto::toDto)
      .orElseThrow(() -> new ApiException(AuthException.NO_MEMBER));
  }

  @Transactional
  public MemberDto updateFcmToken(String memberId, String fcmToken) {
    log.atInfo()
      .addKeyValue("event.action", "member.notification-token.update")
      .log("Member notification token update requested");

    return memberRepository.findById(memberId)
      .map(m -> {
        m.setFcmToken(fcmToken);
        return m;
      })
      .map(MemberDto::toDto)
      .orElseThrow(() -> new ApiException(AuthException.NO_MEMBER));
  }

  @Transactional
  public MemberDto updateAlertAgreement(String memberId, UpdateCommand.Agreements updateAlertAgreement) {
    return memberRepository.findById(memberId)
      .map(m -> m.setAlertAgreements(
        updateAlertAgreement.noticeAgreement(),
        updateAlertAgreement.patchNoteAgreement(),
        updateAlertAgreement.eventAgreement()
      ))
      .map(MemberDto::toDto)
      .orElseThrow(() -> new ApiException(AuthException.NO_MEMBER));
  }

  @Transactional
  public MemberDto updateProfileImageUrl(String memberId, String profileImageUrl) {
    return memberRepository.findById(memberId)
      .map(m -> m.setProfileImageUrl(profileImageUrl))
      .map(MemberDto::toDto)
      .orElseThrow(() -> new ApiException(AuthException.NO_MEMBER));
  }

  @Transactional
  public MemberDto updateProfile(String memberId, Integer level, Integer jobId) {
    Optional<Job> jobOpt = jobRepository.findById(jobId);
    if (jobOpt.isEmpty()) {
      throw ApiException.of(JobException.JOB_NOT_FOUND);
    }

    return memberRepository.findById(memberId)
      .map(m -> {
        m.setLevel(level);
        m.setJobId(jobId);
        return m;
      })
      .map(MemberDto::toDto)
      .orElseThrow(() -> new ApiException(AuthException.NO_MEMBER));
  }

  @Transactional
  public void deleteMember(String memberId) {
    log.atInfo()
      .addKeyValue("event.action", "member.delete")
      .log("Member deletion requested");
    memberRepository.deleteById(memberId);
  }
}
