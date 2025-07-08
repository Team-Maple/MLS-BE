package com.maple.api.item.application.dto;

import com.maple.api.item.domain.Category;

public record CategoryHierarchyDto(
        CategoryDto rootCategory,
        CategoryDto leafCategory
) {
    public static CategoryHierarchyDto toDto(Category rootCategory, Category leafCategory) {
        return new CategoryHierarchyDto(
                CategoryDto.toDto(rootCategory),
                CategoryDto.toDto(leafCategory)
        );
    }
}