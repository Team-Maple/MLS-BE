package com.maple.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(HttpServletRequest request,
                       HttpServletResponse response,
                       AuthenticationException authException)
    throws IOException {
    // 토큰이 유효하지 않을 때 401
    String invalidTokenErrorMsg = "토큰이 유효하지 않음" +
      "\nuri : " + request.getRequestURI() +
      "\nException : " +
      authException.getClass() + "," + authException.getMessage();
//    log.error(invalidTokenErrorMsg);
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, invalidTokenErrorMsg);
  }
}
