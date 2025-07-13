package com.maple.api.item.application.dto;

import com.maple.api.item.domain.Category;

import java.util.List;

public record CategoryDto(
        Integer categoryId,
        String name,
        Integer categoryLevel,
        String description,
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