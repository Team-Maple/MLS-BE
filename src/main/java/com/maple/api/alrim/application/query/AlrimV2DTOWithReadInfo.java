package com.maple.api.alrim.application.query;

import com.maple.api.alrim.domain.Alrim;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "전체 알림 정보 v2인데, 읽었는지 여부를 함께")
public record AlrimV2DTOWithReadInfo(
  AlrimV2DTO alrim,
  @Schema(description = "해당 알림을 현재 유저가 읽었는지 여부")
  boolean alreadyRead
) {
  public static AlrimV2DTOWithReadInfo toDTO(Alrim alrim, boolean alreadyRead) {
    return new AlrimV2DTOWithReadInfo(
      AlrimV2DTO.toDTO(alrim),
      alreadyRead
    );
  }
}
