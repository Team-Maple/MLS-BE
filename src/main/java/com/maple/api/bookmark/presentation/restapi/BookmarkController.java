package com.maple.api.bookmark.presentation.restapi;

import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.bookmark.application.BookmarkService;
import com.maple.api.bookmark.application.CollectionService;
import com.maple.api.bookmark.application.dto.BookmarkAddToCollectionsRequestDto;
import com.maple.api.bookmark.application.dto.BookmarkAddToCollectionsResponseDto;
import com.maple.api.bookmark.application.dto.BookmarkResponseDto;
import com.maple.api.bookmark.application.dto.CreateBookmarkRequestDto;
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
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Bookmark", description = "북마크 관련 API")
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final CollectionService collectionService;

    @PostMapping
    @Operation(
            summary = "북마크 추가",
            description = "아이템, 몬스터, NPC, 퀘스트, 맵에 북마크를 추가합니다.\n\n" +
                    "**리소스 타입:**\n" +
                    "- `ITEM`: 아이템\n" +
                    "- `MONSTER`: 몬스터\n" +
                    "- `NPC`: NPC\n" +
                    "- `QUEST`: 퀘스트\n" +
                    "- `MAP`: 맵"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "북마크 추가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "409", description = "이미 북마크된 리소스"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<BookmarkResponseDto> createBookmark(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody CreateBookmarkRequestDto request) {

        BookmarkResponseDto response = bookmarkService.createBookmark(
                principalDetails.getProviderId(), request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{bookmarkId}/collections")
    @Operation(
            summary = "북마크를 여러 컬렉션에 추가",
            description = "특정 북마크를 여러 컬렉션에 한 번에 추가합니다. 이미 있는 컬렉션에는 추가되지 않습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "컬렉션 추가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "북마크 또는 컬렉션을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 컬렉션에 있는 북마크"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<BookmarkAddToCollectionsResponseDto> addBookmarkToCollections(
            @Parameter(description = "북마크 ID") @PathVariable Integer bookmarkId,
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody BookmarkAddToCollectionsRequestDto request) {

        BookmarkAddToCollectionsResponseDto response = collectionService.addBookmarkToCollections(
                principalDetails.getProviderId(), bookmarkId, request);

        return ResponseEntity.ok(response);
    }
}