package com.maple.api.auth.application.dto;

import com.maple.api.auth.domain.Provider;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreateMemberRequestDto {
  private Provider provider;
  private String providerId;
  @Nullable
  @Size(min = 2, max = 15, message = "닉네임은 2자 이상 15자 이하로 입력해야 합니다.")
  private String nickname;
  @Nullable
  private String fcmToken;
  private Boolean marketingAgreement;
}
