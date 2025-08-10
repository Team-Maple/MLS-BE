package com.maple.api.bookmark.application.dto;

import com.maple.api.bookmark.domain.Collection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "컬렉션 응답")
public record CollectionResponseDto(
        @Schema(description = "컬렉션 ID", example = "1")
        Integer collectionId,

        @Schema(description = "컬렉션 이름", example = "내가 좋아하는 장비")
        String name
) {
    
    public static CollectionResponseDto toDto(Collection collection) {
        return CollectionResponseDto.builder()
                .collectionId(collection.getCollectionId())
                .name(collection.getName())
                .build();
    }
}