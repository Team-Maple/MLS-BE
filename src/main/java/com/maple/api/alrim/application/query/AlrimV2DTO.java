package com.maple.api.alrim.application.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.maple.api.alrim.domain.Alrim;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "알림 정보 v2")
public record AlrimV2DTO(
  @Schema(description = "알림 ID")
  Long id,
  @Schema(description = "공지사항 패치노트 업데이트")
  String type,
  @Schema(description = "제목")
  String title,
  @Schema(description = "알림 링크")
  String link,
  @Schema(description = "등록일자")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  LocalDateTime date
) {
  public static AlrimV2DTO toDTO(Alrim alrim) {
    return new AlrimV2DTO(
      alrim.getId(),
      alrim.getType().getLabel(),
      alrim.getTitle(),
      alrim.getLink(),
      alrim.getDate()
    );
  }
}
