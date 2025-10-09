package com.maple.api.auth.application.dto;

import com.maple.api.auth.domain.Member;
import com.maple.api.auth.domain.Provider;

public record MemberDto(
  String id,
  Provider provider,
  String nickname,
  String fcmToken, // 원래 없어야 하나, FE 개발의 편의를 위해 잠시 추가~
  Boolean marketingAgreement,
  Boolean noticeAgreement,
  Boolean patchNoteAgreement,
  Boolean eventAgreement,
  //
  Integer jobId,
  Integer level,
  String profileImageUrl
) {
  public static MemberDto toDto(Member member) {
    return new MemberDto(
      member.getId(),
      member.getProvider(),
      member.getNickname(),
      member.getFcmToken(),
      member.getMarketingAgreement(),
      member.getNoticeAgreement(),
      member.getPatchNoteAgreement(),
      member.getEventAgreement(),
      //
      member.getJobId(),
      member.getLevel(),
      member.getProfileImageUrl()
    );
  }
}
