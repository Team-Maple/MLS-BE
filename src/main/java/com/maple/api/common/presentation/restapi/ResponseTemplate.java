package com.maple.api.common.presentation.restapi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class ResponseTemplate<T> {
  private boolean success;
  private String code;  // "SUCCESS" or specific error code
  private String message;
  private T data;

  public static <T> ResponseTemplate<T> success(T data) {
    return ResponseTemplate.of(true, "SUCCESS", null, data);
  }

  public static <T> ResponseTemplate<T> failure(String code, String message) {
    return ResponseTemplate.of(false, code, message, null);
  }
}
