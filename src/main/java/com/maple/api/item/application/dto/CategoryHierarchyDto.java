package com.maple.api.item.application.dto;

public record CategoryHierarchyDto(
        CategorySimpleDto rootCategory,
        CategorySimpleDto leafCategory
) {
    public static CategoryHierarchyDto of(CategorySimpleDto rootCategory, CategorySimpleDto leafCategory) {
        return new CategoryHierarchyDto(rootCategory, leafCategory);
    }
}
