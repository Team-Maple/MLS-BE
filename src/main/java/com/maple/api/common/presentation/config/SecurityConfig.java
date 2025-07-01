package com.maple.api.common.presentation.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
  private final JwtSecurityAdapter jwtSecurityAdapter;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
      // exception handling 할 때 우리가 만든 클래스를 추가
      .exceptionHandling(c ->
        c.authenticationEntryPoint(jwtAuthenticationEntryPoint)
          .accessDeniedHandler(jwtAccessDeniedHandler))
      // 시큐리티는 기본적으로 세션을 사용
      // JWT 토큰과 Oauth 토큰은 세션을 사용하지 않기 때문에 세션 설정을 Stateless 로 설정
      .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      // 로그인, 회원가입 API 는 토큰이 없는 상태에서 요청이 들어오기 때문에 permitAll 설정
      .authorizeHttpRequests(customizer ->
        customizer
          .requestMatchers("/actuator/health").permitAll()
          .requestMatchers("/actuator/info").permitAll()
          .requestMatchers("/api/v1/auth/login/**").permitAll()
          .requestMatchers("/api/v1/auth/signup/**").permitAll()
          .requestMatchers("/swagger-ui-maple/**").permitAll()
          .requestMatchers("/v3/api-docs/**").permitAll()
          .requestMatchers("/api/v1/search/**").permitAll()
          .requestMatchers("/api/v1/items/**").permitAll()
          .anyRequest().authenticated()
      )
      .with(jwtSecurityAdapter, Customizer.withDefaults());

    return http.build();
  }

//  @Bean
//  public CorsConfigurationSource corsConfigurationSource() {
//    CorsConfiguration configuration = new CorsConfiguration();
//    configuration.addAllowedOrigin("*"); // 필요한 Origin만 허용
//    configuration.addAllowedMethod("*"); // 모든 HTTP 메서드 허용
//    configuration.addAllowedHeader("*"); // 모든 헤더 허용
//    configuration.setAllowCredentials(true); // 자격 증명 허용
//
//    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//    source.registerCorsConfiguration("/**", configuration);
//    return source;
//  }
}
