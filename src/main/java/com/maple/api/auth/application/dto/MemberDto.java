package com.maple.api.auth.application.dto;

import com.maple.api.auth.domain.Member;
import com.maple.api.auth.domain.Provider;

public record MemberDto(String id, Provider provider, String nickname) {
  public static MemberDto toDto(Member member) {
    return new MemberDto(member.getId(), member.getProvider(), member.getNickname());
  }
}
