package com.maple.api.common.presentation.config;

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
    log.atInfo()
      .addKeyValue("event.action", "http.request.unauthenticated")
      .addKeyValue("event.outcome", "failure")
      .addKeyValue("http.request.method", request.getMethod())
      .addKeyValue("http.response.status_code", HttpServletResponse.SC_UNAUTHORIZED)
      .addKeyValue("error.type", authException.getClass().getName())
      .log("Request authentication failed");
    String invalidTokenErrorMessage = "토큰이 유효하지 않음" +
      "\nuri : " + request.getRequestURI() +
      "\nException : " + authException.getClass() + "," + authException.getMessage();
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, invalidTokenErrorMessage);
  }
}
