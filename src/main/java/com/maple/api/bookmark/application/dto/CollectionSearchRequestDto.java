package com.maple.api.bookmark.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.List;

@Schema(description = "컬렉션 목록 및 선택한 북마크 포함 상태 조회 요청")
public record CollectionSearchRequestDto(
        @Schema(description = "포함 여부를 확인할 북마크 ID 목록. 생략하면 포함 상태를 반환하지 않습니다.",
                example = "[101, 102, 103]")
        @Size(min = 1, message = "최소 1개 이상의 북마크 ID가 필요합니다.")
        @UniqueElements(message = "중복된 북마크 ID가 있습니다.")
        List<@NotNull @Positive Integer> bookmarkIds
) {
}
