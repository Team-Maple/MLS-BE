package com.maple.api.bookmark.presentation.restapi;

import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.bookmark.application.BookmarkQueryService;
import com.maple.api.bookmark.application.CollectionQueryService;
import com.maple.api.bookmark.application.CollectionService;
import com.maple.api.bookmark.application.dto.BookmarkSummaryDto;
import com.maple.api.bookmark.application.dto.CollectionAddBookmarksRequestDto;
import com.maple.api.bookmark.application.dto.CollectionAddBookmarksResponseDto;
import com.maple.api.bookmark.application.dto.CollectionResponseDto;
import com.maple.api.bookmark.application.dto.CollectionWithBookmarksDto;
import com.maple.api.bookmark.application.dto.CreateCollectionRequestDto;
import com.maple.api.common.presentation.restapi.ResponseTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
@Tag(name = "Collection", description = "컬렉션 관련 API")
public class CollectionController {

    private final CollectionService collectionService;
    private final CollectionQueryService collectionQueryService;
    private final BookmarkQueryService bookmarkQueryService;

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

    @DeleteMapping("/{collectionId}")
    @Operation(
            summary = "컬렉션 삭제",
            description = "특정 컬렉션과 해당 컬렉션에 포함된 북마크 관계를 삭제합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "컬렉션 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "컬렉션을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseTemplate<Void>> deleteCollection(
            @Parameter(description = "컬렉션 ID") @PathVariable Integer collectionId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        collectionService.deleteCollection(principalDetails.getProviderId(), collectionId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{collectionId}/bookmarks")
    @Operation(
            summary = "컬렉션별 북마크 조회",
            description = "특정 컬렉션에 포함된 북마크들을 조회합니다. 정렬과 페이징 옵션을 제공합니다.\\n\\n" +
                    "**정렬 옵션:**\\n" +
                    "- `createdAt`: 북마크 생성순 (최신순/오래된순)\\n" +
                    "- `name`: 북마크명 가나다순\\n\\n" +
                    "**정렬 사용 예시:**\\n" +
                    "- `sort=createdAt,desc`: 최신 북마크순\\n" +
                    "- `sort=name,asc`: 가나다순\\n" +
                    "- 기본값: 최신 북마크순"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "컬렉션별 북마크 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (컬렉션 소유자가 아님)"),
            @ApiResponse(responseCode = "404", description = "컬렉션을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Page<BookmarkSummaryDto>> getCollectionBookmarks(
            @Parameter(description = "컬렉션 ID") @PathVariable Integer collectionId,
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        // 컬렉션 소유권 검증
        collectionService.validateCollectionOwnership(principalDetails.getProviderId(), collectionId);

        // 컬렉션별 북마크 조회
        Page<BookmarkSummaryDto> collectionBookmarks = bookmarkQueryService.getCollectionBookmarks(
                principalDetails.getProviderId(), collectionId, pageable);

        return ResponseEntity.ok(collectionBookmarks);
    }

    @GetMapping
    @Operation(
            summary = "컬렉션 목록 조회",
            description = "사용자의 컬렉션 목록을 조회합니다. 각 컬렉션마다 최신 4개의 북마크 정보를 포함합니다.\\n\\n" +
                    "**정렬 옵션:**\\n" +
                    "- `createdAt`: 컬렉션 생성순 (최신순/오래된순)\\n" +
                    "- `name`: 컬렉션명 가나다순\\n\\n" +
                    "**정렬 사용 예시:**\\n" +
                    "- `sort=createdAt,desc`: 최신 컬렉션순 (기본값)\\n" +
                    "- `sort=name,asc`: 가나다순\\n" +
                    "- `sort=createdAt,asc`: 오래된 컬렉션순"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "컬렉션 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Page<CollectionWithBookmarksDto>> getCollections(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        Page<CollectionWithBookmarksDto> collections = collectionQueryService.getCollectionsWithRecentBookmarks(
                principalDetails.getProviderId(), pageable);

        return ResponseEntity.ok(collections);
    }
}