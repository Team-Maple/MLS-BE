package com.maple.api.bookmark.presentation.restapi;

import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.bookmark.application.CollectionService;
import com.maple.api.bookmark.application.dto.CollectionAddBookmarksRequestDto;
import com.maple.api.bookmark.application.dto.CollectionAddBookmarksResponseDto;
import com.maple.api.bookmark.application.dto.CollectionResponseDto;
import com.maple.api.bookmark.application.dto.CreateCollectionRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
@Tag(name = "Collection", description = "컬렉션 관련 API")
public class CollectionController {

    private final CollectionService collectionService;

    @PostMapping
    @Operation(
            summary = "컬렉션 생성",
            description = "새로운 북마크 컬렉션을 생성합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "컬렉션 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CollectionResponseDto> createCollection(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody CreateCollectionRequestDto request) {

        CollectionResponseDto response = collectionService.createCollection(
                principalDetails.getProviderId(), request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{collectionId}/bookmarks")
    @Operation(
            summary = "컬렉션에 북마크 추가",
            description = "특정 컬렉션에 여러 북마크를 한 번에 추가합니다. 중복된 북마크는 자동으로 필터링됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "북마크 추가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "컬렉션 또는 북마크를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CollectionAddBookmarksResponseDto> addBookmarksToCollection(
            @Parameter(description = "컬렉션 ID") @PathVariable Integer collectionId,
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody CollectionAddBookmarksRequestDto request) {

        CollectionAddBookmarksResponseDto response = collectionService.addBookmarksToCollection(
                principalDetails.getProviderId(), collectionId, request);

        return ResponseEntity.ok(response);
    }
}