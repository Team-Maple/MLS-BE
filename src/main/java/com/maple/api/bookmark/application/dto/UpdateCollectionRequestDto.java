package com.maple.api.bookmark.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "컬렉션 수정 요청")
public record UpdateCollectionRequestDto(
        @NotBlank(message = "컬렉션 이름은 필수입니다.")
        @Size(min = 1, max = 18, message = "컬렉션 이름은 1자 이상 18자 이하여야 합니다.")
        @Schema(description = "수정할 컬렉션 이름", example = "업데이트된 컬렉션 이름")
        String name
) {}

