package com.maple.api.item.application.dto;

import com.maple.api.item.domain.Category;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "하위 카테고리 정보 (children 미포함)")
public record CategorySimpleDto(
        @Schema(description = "카테고리 ID", example = "2")
        Integer categoryId,

        @Schema(description = "카테고리 이름", example = "무기")
        String name,

        @Schema(description = "카테고리 레벨 (계층 구조)", example = "1")
        Integer categoryLevel,

        @Schema(description = "카테고리 설명", example = "모든 종류의 무기")
        String description
) {
    public static CategorySimpleDto from(Category category) {
        if (category == null) {
            return null;
        }

        return new CategorySimpleDto(
                category.getCategoryId(),
                category.getName(),
                category.getCategoryLevel(),
                category.getDescription()
        );
    }

    public static CategorySimpleDto from(CategoryDto categoryDto) {
        if (categoryDto == null) {
            return null;
        }

        return new CategorySimpleDto(
                categoryDto.categoryId(),
                categoryDto.name(),
                categoryDto.categoryLevel(),
                categoryDto.description()
        );
    }
}
