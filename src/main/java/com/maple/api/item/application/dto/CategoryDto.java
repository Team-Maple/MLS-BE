package com.maple.api.item.application.dto;

import com.maple.api.item.domain.Category;

public record CategoryDto(
        Integer categoryId,
        String name,
        Integer parentCategoryId,
        Integer categoryLevel,
        String description
) {
    public static CategoryDto toDto(Category category) {
        if (category == null) {
            return null;
        }
        
        return new CategoryDto(
                category.getCategoryId(),
                category.getName(),
                category.getParentCategoryId(),
                category.getCategoryLevel(),
                category.getDescription()
        );
    }
}