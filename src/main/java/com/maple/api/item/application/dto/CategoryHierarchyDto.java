package com.maple.api.item.application.dto;

public record CategoryHierarchyDto(
        CategoryDto rootCategory,
        CategoryDto leafCategory
) {
    public static CategoryHierarchyDto of(CategoryDto rootCategory, CategoryDto leafCategory) {
        return new CategoryHierarchyDto(rootCategory, leafCategory);
    }
}
