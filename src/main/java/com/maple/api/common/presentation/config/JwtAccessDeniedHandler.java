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
    // 필요한 권한이 없이 접근하려 할때 403
    String accessDeniedErrorMsg = "필요한 권한이 없음" +
      "\nuri : " + request.getRequestURI() +
      "\nException : " +
      accessDeniedException.getClass() + "," + accessDeniedException.getMessage();
    log.error(accessDeniedErrorMsg);
    response.sendError(HttpServletResponse.SC_FORBIDDEN, accessDeniedErrorMsg);
  }
}
