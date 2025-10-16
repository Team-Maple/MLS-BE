package com.maple.api.item.application;

import com.maple.api.common.presentation.exception.ApiException;
import com.maple.api.item.application.dto.CategoryDto;
import com.maple.api.item.application.dto.CategorySimpleDto;
import com.maple.api.item.domain.Category;
import com.maple.api.item.exception.ItemException;
import com.maple.api.item.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final Map<Integer, Category> categoryCache = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> rootCategoryCache = new ConcurrentHashMap<>();
    private final Map<Integer, CategoryDto> categoryDtoCache = new ConcurrentHashMap<>();
    private List<CategoryDto> categoryTreeCache;

    @PostConstruct
    public void initializeCache() {
        List<Category> categories = categoryRepository.findAll();

        for (Category category : categories) {
            categoryCache.put(category.getCategoryId(), category);
        }

        for (Category category : categories) {
            Integer rootCategoryId = findRootCategoryId(category.getCategoryId());
            rootCategoryCache.put(category.getCategoryId(), rootCategoryId);
        }

        this.categoryTreeCache = buildAndCacheCategoryTree();
    }

    public Category findById(Integer categoryId) {
        Category category = categoryCache.get(categoryId);
        if (category == null) {
            throw ApiException.of(ItemException.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    public Category findRootCategory(Integer categoryId) {
        Integer rootCategoryId = rootCategoryCache.get(categoryId);
        if (rootCategoryId == null) {
            throw ApiException.of(ItemException.CATEGORY_NOT_FOUND);
        }
        Category rootCategory = categoryCache.get(rootCategoryId);
        if (rootCategory == null) {
            throw ApiException.of(ItemException.CATEGORY_NOT_FOUND);
        }
        return rootCategory;
    }

    private Integer findRootCategoryId(Integer categoryId) {
        Category current = categoryCache.get(categoryId);
        if (current == null) {
            throw ApiException.of(ItemException.CATEGORY_NOT_FOUND);
        }
        
        while (current.getParentCategoryId() != null) {
            current = categoryCache.get(current.getParentCategoryId());
            if (current == null) {
                throw ApiException.of(ItemException.PARENT_CATEGORY_NOT_FOUND);
            }
        }
        
        return current.getCategoryId();
    }

    public List<CategoryDto> getAllCategories() {
        return this.categoryTreeCache;
    }

    private List<CategoryDto> buildAndCacheCategoryTree() {
        categoryDtoCache.clear();

        List<Category> enabledCategories = categoryCache.values().stream()
                .filter(category -> !category.isDisabled())
                .toList();

        Map<Integer, List<Category>> categoryByParent = enabledCategories.stream()
                .collect(Collectors.groupingBy(
                        category -> category.getParentCategoryId() != null ? category.getParentCategoryId() : 0
                ));

        List<Category> rootCategories = categoryByParent.getOrDefault(0, Collections.emptyList());

        return rootCategories.stream()
                .map(rootCategory -> buildCategoryTree(rootCategory, categoryByParent, new HashSet<>()))
                .toList();
    }

    private CategoryDto buildCategoryTree(Category category, Map<Integer, List<Category>> categoryByParent, Set<Integer> visited) {
        if (!visited.add(category.getCategoryId())) {
            log.warn("Circular reference detected for categoryId: {}", category.getCategoryId());
            return CategoryDto.toDto(category, Collections.emptyList());
        }

        List<Category> children = categoryByParent.getOrDefault(category.getCategoryId(), Collections.emptyList());
        
        List<CategoryDto> childrenDtos = children.stream()
                .map(child -> buildCategoryTree(child, categoryByParent, visited))
                .toList();

        visited.remove(category.getCategoryId());

        CategoryDto dto = CategoryDto.toDto(category, childrenDtos);
        categoryDtoCache.put(category.getCategoryId(), dto);
        return dto;
    }

    public CategoryDto findCategoryDto(Integer categoryId) {
        CategoryDto cachedDto = categoryDtoCache.get(categoryId);
        if (cachedDto != null) {
            return cachedDto;
        }

        Category category = categoryCache.get(categoryId);
        if (category == null) {
            throw ApiException.of(ItemException.CATEGORY_NOT_FOUND);
        }

        CategoryDto dto = CategoryDto.toDto(category, Collections.emptyList());
        categoryDtoCache.put(categoryId, dto);
        return dto;
    }

    public CategoryDto findRootCategoryDto(Integer categoryId) {
        Integer rootCategoryId = rootCategoryCache.get(categoryId);
        if (rootCategoryId == null) {
            throw ApiException.of(ItemException.CATEGORY_NOT_FOUND);
        }

        return findCategoryDto(rootCategoryId);
    }

    public CategorySimpleDto findCategorySimpleDto(Integer categoryId) {
        Category category = categoryCache.get(categoryId);
        if (category == null) {
            throw ApiException.of(ItemException.CATEGORY_NOT_FOUND);
        }

        return CategorySimpleDto.from(category);
    }

    public CategorySimpleDto findRootCategorySimpleDto(Integer categoryId) {
        Integer rootCategoryId = rootCategoryCache.get(categoryId);
        if (rootCategoryId == null) {
            throw ApiException.of(ItemException.CATEGORY_NOT_FOUND);
        }

        return findCategorySimpleDto(rootCategoryId);
    }
}
