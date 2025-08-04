package com.maple.api.auth.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateCommand {
  public record NickName(
    @Size(min = 2, max = 15, message = "닉네임은 2자 이상 15자 이하로 입력해야 합니다.")
    String nickname
  ) {
  }

  public record MarketingAgreement(
    Boolean marketingAgreement
  ) {
  }

  public record Agreements(
    Boolean noticeAgreement,
    Boolean patchNoteAgreement,
    Boolean eventAgreement
  ) {
  }


  public record FcmToken(
    String fcmToken
  ) {
  }

  //  @Data
//  @AllArgsConstructor
//  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public record Profile(
    @Min(value = 1, message = "레벨은 1 이상의 값을 입력해야 합니다.")
    @Max(value = 200, message = "레벨은 200 이하의 값을 입력해야 합니다.")
    Integer level,
    Integer jobId
  ) {
  }
}
