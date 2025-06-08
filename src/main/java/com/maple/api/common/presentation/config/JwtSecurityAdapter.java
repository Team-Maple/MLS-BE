package com.maple.api.common.presentation.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

// 직접 만든 TokenProviderImpl 와 JwtFilter 를 SecurityConfig 에 적용할 때 사용
@Component
@RequiredArgsConstructor
public class JwtSecurityAdapter extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {
  private final JwtTokenValidator jwtTokenValidator;

  // TokenProviderImpl 를 주입받아서 JwtFilter 를 통해 Security 로직에 필터를 등록
  @Override
  public void configure(HttpSecurity http) {
    JwtAuthFilter customFilter = new JwtAuthFilter(jwtTokenValidator);
    http.addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class);
  }
}
