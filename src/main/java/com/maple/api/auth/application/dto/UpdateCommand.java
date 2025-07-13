package com.maple.api.auth.application.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateCommand {
  @Data
  @AllArgsConstructor
  public static class NickName {
    @Size(min = 2, max = 15, message = "닉네임은 2자 이상 15자 이하로 입력해야 합니다.")
    String nickname;
  }

  @Data
  @AllArgsConstructor
  public static class MarketingAgreement {
    Boolean marketingAgreement;
  }

  @Data
  @AllArgsConstructor
  public static class Agreements {
    Boolean noticeAgreement;
    Boolean patchNoteAgreement;
    Boolean eventAgreement;
  }

  @Data
  @AllArgsConstructor
  public static class FcmToken {
    String fcmToken;
  }
}
