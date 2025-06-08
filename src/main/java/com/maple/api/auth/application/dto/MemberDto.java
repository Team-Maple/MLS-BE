package com.maple.api.auth.application.dto;

import com.maple.api.auth.domain.Member;
import com.maple.api.auth.domain.Provider;

public record MemberDto(String id, String email, Provider provider) {
  public static MemberDto toDto(Member member) {
    return new MemberDto(member.getId(), member.getEmail(), member.getProvider());
  }
}
