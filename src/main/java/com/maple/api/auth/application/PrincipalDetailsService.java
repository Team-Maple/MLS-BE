package com.maple.api.auth.application;

import com.maple.api.auth.domain.Member;
import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.auth.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PrincipalDetailsService implements UserDetailsService {
  private final MemberRepository memberRepository;

  @Override
  public UserDetails loadUserByUsername(String providerId) throws UsernameNotFoundException {
    Optional<Member> memberOpt = memberRepository.findById(providerId);
    return memberOpt.map(it -> new PrincipalDetails(it.getId())).orElse(null);
  }
}
