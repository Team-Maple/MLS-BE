package com.maple.api.alrim.application.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.maple.api.alrim.domain.Alrim;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "알림 정보")
public record AlrimDTO(
  @Schema(description = "공지사항 패치노트 업데이트")
  String type,
  @Schema(description = "제목")
  String title,
  @Schema(description = "알림정보 LINK. 알림의 ID 로 사용하며, 알림을 읽었는지를 판단하는 기준이 됩니다.")
  String link,
  @Schema(description = "등록일자")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  LocalDateTime date
) {
  public static AlrimDTO toDTO(Alrim alrim) {
    return new AlrimDTO(
      alrim.getType().getLabel(),
      alrim.getTitle(),
      alrim.getLink(),
      alrim.getDate()
    );
  }
}
