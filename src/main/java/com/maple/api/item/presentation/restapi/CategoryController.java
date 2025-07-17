package com.maple.api.item.presentation.restapi;

import com.maple.api.common.presentation.restapi.ResponseTemplate;
import com.maple.api.item.application.CategoryService;
import com.maple.api.item.application.dto.CategoryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category", description = "아이템 카테고리 관련 API")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(
        summary = "모든 카테고리 조회",
        description = "시스템에서 사용 가능한 모든 아이템 카테고리 목록을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "카테고리 목록 조회 성공"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseTemplate<List<CategoryDto>>> getAllCategories() {
        return ResponseEntity.ok(ResponseTemplate.success(categoryService.getAllCategories()));
    }
}