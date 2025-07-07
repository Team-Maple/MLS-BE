package com.maple.api.auth.application.dto;

import com.maple.api.auth.domain.Provider;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.lang.Nullable;

@Data
@AllArgsConstructor
public class CreateMemberRequestDto {
  private Provider provider;
  private String providerId;
  @Nullable private String nickname;
  private Boolean marketingAgreement;
  @Nullable private String fcmToken;
}
