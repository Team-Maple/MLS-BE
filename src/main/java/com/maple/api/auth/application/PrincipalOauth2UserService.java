package com.maple.api.auth.application;

import com.maple.api.auth.domain.Member;
import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.auth.domain.Provider;
import com.maple.api.auth.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class PrincipalOauth2UserService extends DefaultOAuth2UserService {

//  @Autowired
//  private BCryptPasswordEncoder bCryptPasswordEncoder;

  @Autowired
  private MemberRepository memberRepository;

  // 구글로부터 받은 userRequest 데이터에 대한 후처리되는 함수
  // 함수 종료시 @AuthenticationPrincipal 어노테이션이 만들어진다.
  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    log.debug("clientRegistration {} / accessToken {} ", userRequest.getClientRegistration(), userRequest.getAccessToken().getTokenValue());

    OAuth2User oAuth2User = super.loadUser(userRequest);
    // 구글로그인 버튼 클릭 -> 구글로그인창 -> 로그인을 완료 -> code를 리턴(OAuth2-Client 라이브러리) -> AccessToken 요청
    // userRequest 정보 -> 회원 프로필 받아야함(loadUser함수 호출) -> 구글로부터 회원프로필 받아준다.
    log.debug("attributes: {} ", oAuth2User.getAttributes());

    String provider = userRequest.getClientRegistration().getRegistrationId();

    switch (provider) {
      case "kakao":
        String providerId = ((Long) Objects.requireNonNull(oAuth2User.getAttribute("id"))).toString();
        Optional<Member> memberOpt = memberRepository.findById(providerId);

        if (memberOpt.isPresent()) return new PrincipalDetails(memberOpt.get(), oAuth2User.getAttributes());

        String email = ((Map<String, String>) oAuth2User.getAttribute("kakao_account")).get("email");
        log.info("카카오 로그인 요청");

        return new PrincipalDetails(memberRepository.save(new Member(
          providerId,
          email,
          Provider.KAKAO
        )), oAuth2User.getAttributes());
      case "apple":
        log.info("애플 로그인 요청");
      default:
        throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 제공자입니다.");
    }
  }
}