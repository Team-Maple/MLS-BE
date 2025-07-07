package com.maple.api.auth.domain;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Map;

@Data
public class PrincipalDetails implements UserDetails {
  private final String providerId;
  private Collection<GrantedAuthority> authorities;
  private Map<String, Object> attributes;

  // 일반 로그인
  public PrincipalDetails(String providerId) {
    this.providerId = providerId;
  }

  public PrincipalDetails(String providerId, Collection<GrantedAuthority> authorities, Map<String, Object> attributes) {
    this.providerId = providerId;
    this.authorities = authorities;
    this.attributes = attributes;
  }


  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return "";
  }

  @Override
  public String getUsername() {
    return providerId;
  }
}
