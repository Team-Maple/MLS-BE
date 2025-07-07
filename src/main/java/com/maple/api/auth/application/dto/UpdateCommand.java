package com.maple.api.auth.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateCommand {
  @Data
  @AllArgsConstructor
  public static class NickName {
    String providerId;
    String nickname;
  }

  @Data
  @AllArgsConstructor
  public static class MarketingAgreement {
    String providerId;
    Boolean marketingAgreement;
  }

  @Data
  @AllArgsConstructor
  public static class Agreements {
    String providerId;
    Boolean noticeAgreement;
    Boolean patchNoteAgreement;
    Boolean eventAgreement;
  }

  @Data
  @AllArgsConstructor
  public static class FcmToken {
    String providerId;
    String fcmToken;
  }
}
