package com.maple.api.item.application;

import com.maple.api.item.application.dto.CategoryDto;
import com.maple.api.item.domain.Category;
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
            throw new IllegalArgumentException("Category not found id: " + categoryId);
        }
        return category;
    }

    public Category findRootCategory(Integer categoryId) {
        Integer rootCategoryId = rootCategoryCache.get(categoryId);
        if (rootCategoryId == null) {
            throw new IllegalArgumentException("Root category not found child id: " + categoryId);
        }
        Category rootCategory = categoryCache.get(rootCategoryId);
        if (rootCategory == null) {
            throw new IllegalArgumentException("Root category not found id: " + rootCategoryId);
        }
        return rootCategory;
    }

    private Integer findRootCategoryId(Integer categoryId) {
        Category current = categoryCache.get(categoryId);
        if (current == null) {
            return null;
        }
        
        while (current.getParentCategoryId() != null) {
            current = categoryCache.get(current.getParentCategoryId());
            if (current == null) {
                break;
            }
        }
        
        return current != null ? current.getCategoryId() : null;
    }

    public List<CategoryDto> getAllCategories() {
        return this.categoryTreeCache;
    }

    private List<CategoryDto> buildAndCacheCategoryTree() {
        List<Category> enabledCategories = categoryCache.values().stream()
                .filter(Category::isEnabled)
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

        return CategoryDto.toDto(category, childrenDtos);
    }
}