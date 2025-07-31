package com.maple.api.alrim.application.query;

import com.maple.api.alrim.domain.Alrim;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "전체 알림 정보인데, 읽었는지 여부를 함께")
public record AlrimDTOWithReadInfo(
  AlrimDTO alrim,
  @Schema(description = "해당 알림을 현재 유저가 읽었는지 여부")
  boolean alreadyRead
) {
  public static AlrimDTOWithReadInfo toDTO(Alrim alrim, boolean alreadyRead) {
    return new AlrimDTOWithReadInfo(
      AlrimDTO.toDTO(alrim),
      alreadyRead
    );
  }
}
