package com.maple.api.auth.domain;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Data
public class PrincipalDetails implements UserDetails
{
  private final Member member;
  private Map<String, Object> attributes;

  // 일반 로그인
  public PrincipalDetails(Member member) {
    this.member = member;
  }

  // OAuth 로그인
  public PrincipalDetails(Member member, Map<String, Object> attributes) {
    this.member = member;
    this.attributes = attributes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new GrantedAuthority() {
      @Override
      public String getAuthority() {
        return member.getRole();
      }
    });
  }

  @Override
  public String getPassword() {
    return "";
  }

  @Override
  public String getUsername() {
    return member.getEmail();
  }
}
