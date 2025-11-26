package com.maple.api.bookmark.presentation.restapi;

import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.bookmark.application.CollectionService;
import com.maple.api.bookmark.application.dto.BookmarkCollectionBulkAddRequestDto;
import com.maple.api.bookmark.application.dto.BookmarkCollectionBulkAddResponseDto;
import com.maple.api.common.presentation.restapi.ResponseTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookmark-collections")
@RequiredArgsConstructor
@Tag(name = "BookmarkCollection", description = "컬렉션-북마크 매핑 관련 API")
public class BookmarkCollectionController {

    private final CollectionService collectionService;

    @PostMapping
    @Operation(
            summary = "여러 컬렉션에 여러 북마크 추가",
            description = "여러 컬렉션에 여러 북마크를 한 번에 추가합니다. 이미 존재하는 매핑은 무시하고 신규 매핑만 추가합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "북마크 추가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "컬렉션 또는 북마크를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 북마크-컬렉션 매핑"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseTemplate<BookmarkCollectionBulkAddResponseDto>> addBookmarksToCollections(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody BookmarkCollectionBulkAddRequestDto request
    ) {

        BookmarkCollectionBulkAddResponseDto response = collectionService.addBookmarksToCollections(
                principalDetails.getProviderId(), request);

        return ResponseEntity.ok(ResponseTemplate.success(response));
    }
}
