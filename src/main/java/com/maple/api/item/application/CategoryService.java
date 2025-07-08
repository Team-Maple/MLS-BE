package com.maple.api.item.application;

import com.maple.api.item.domain.Category;
import com.maple.api.item.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final Map<Integer, Category> categoryCache = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> rootCategoryCache = new ConcurrentHashMap<>();

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
    }

    public Optional<Category> findById(Integer categoryId) {
        return Optional.ofNullable(categoryCache.get(categoryId));
    }

    public Optional<Category> findRootCategory(Integer categoryId) {
        Integer rootCategoryId = rootCategoryCache.get(categoryId);
        if (rootCategoryId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(categoryCache.get(rootCategoryId));
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
}