package com.maple.api.common.presentation.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

  @Override
  public void handle(HttpServletRequest request,
                     HttpServletResponse response,
                     AccessDeniedException accessDeniedException)
    throws IOException {
    log.atWarn()
      .addKeyValue("event.action", "http.request.denied")
      .addKeyValue("event.outcome", "failure")
      .addKeyValue("http.request.method", request.getMethod())
      .addKeyValue("http.response.status_code", HttpServletResponse.SC_FORBIDDEN)
      .addKeyValue("error.type", accessDeniedException.getClass().getName())
      .log("Request authorization denied");
    String accessDeniedErrorMessage = "필요한 권한이 없음" +
      "\nuri : " + request.getRequestURI() +
      "\nException : " + accessDeniedException.getClass() + "," + accessDeniedException.getMessage();
    response.sendError(HttpServletResponse.SC_FORBIDDEN, accessDeniedErrorMessage);
  }
}
