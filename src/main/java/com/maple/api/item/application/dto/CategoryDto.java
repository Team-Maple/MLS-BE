package com.maple.api.item.application.dto;

import com.maple.api.item.domain.Category;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "카테고리 정보")
public record CategoryDto(
        @Schema(description = "카테고리 ID", example = "2")
        Integer categoryId,
        
        @Schema(description = "카테고리 이름", example = "무기")
        String name,
        
        @Schema(description = "카테고리 레벨 (계층 구조)", example = "1")
        Integer categoryLevel,
        
        @Schema(description = "카테고리 설명", example = "모든 종류의 무기")
        String description,
        
        @Schema(description = "하위 카테고리 목록")
        List<CategoryDto> children
) {
    public static CategoryDto toDto(Category category) {
        return toDto(category, null);
    }

    public static CategoryDto toDto(Category category, List<CategoryDto> children) {
        if (category == null) {
            return null;
        }
        
        return new CategoryDto(
                category.getCategoryId(),
                category.getName(),
                category.getCategoryLevel(),
                category.getDescription(),
                children
        );
    }
}