package com.maple.api.item.presentation.restapi;

import com.maple.api.common.presentation.restapi.ResponseTemplate;
import com.maple.api.item.application.CategoryService;
import com.maple.api.item.application.dto.CategoryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ResponseTemplate<List<CategoryDto>>> getAllCategories() {
        return ResponseEntity.ok(ResponseTemplate.success(categoryService.getAllCategories()));
    }
}