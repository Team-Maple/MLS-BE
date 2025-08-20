package com.maple.api.bookmark.presentation.restapi;

import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.bookmark.application.BookmarkQueryService;
import com.maple.api.bookmark.application.BookmarkService;
import com.maple.api.bookmark.application.CollectionService;
import com.maple.api.bookmark.application.dto.*;
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
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Bookmark", description = "북마크 관련 API")
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final BookmarkQueryService bookmarkQueryService;
    private final CollectionService collectionService;

    @GetMapping
    @Operation(
            summary = "북마크 전체 조회",
            description = "사용자가 소지하고 있는 북마크 전체를 조회합니다.\n\n" +
                    "**페이징:**\n" +
                    "- 기본 사이즈: 200개\n\n" +
                    "**정렬 기준:**\n" +
                    "- createdAt: 북마크 생성순 정렬 (최신순 기본값)\n" +
                    "- name: 이름순 정렬\n" +
                    "- 페이지 크기: 20개 (기본값)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "북마크 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Page<BookmarkSummaryDto>> getBookmarks(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {

        Page<BookmarkSummaryDto> bookmarks = bookmarkQueryService.getBookmarks(
                principalDetails.getProviderId(), pageable);

        return ResponseEntity.ok(bookmarks);
    }

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

    @GetMapping("/items")
    @Operation(
            summary = "아이템 북마크 조회",
            description = "사용자가 북마크한 아이템들을 조회합니다. 다양한 필터링과 정렬 옵션을 제공합니다.\n\n" +
                    "**필터링 옵션:**\n" +
                    "- `jobIds`: 직업 ID 목록 (특정 직업이 착용 가능한 아이템만 검색)\n" +
                    "- `minLevel`, `maxLevel`: 요구 레벨 범위\n" +
                    "- `categoryIds`: 카테고리 ID 목록\n\n" +
                    "**정렬 옵션:**\n" +
                    "- `createdAt`: 북마크 생성순 (최신순/오래된순)\n" +
                    "- `name`: 아이템명 가나다순\n\n" +
                    "**정렬 사용 예시:**\n" +
                    "- `sort=createdAt,desc`: 최신 북마크순\n" +
                    "- `sort=name,asc`: 가나다순\n" +
                    "- 기본값: 최신 북마크순"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "아이템 북마크 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Page<BookmarkSummaryDto>> getItemBookmarks(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ParameterObject ItemBookmarkSearchRequestDto searchRequest,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        Page<BookmarkSummaryDto> itemBookmarks = bookmarkQueryService.getItemBookmarks(
                principalDetails.getProviderId(), searchRequest, pageable);

        return ResponseEntity.ok(itemBookmarks);
    }

    @GetMapping("/monsters")
    @Operation(
            summary = "몬스터 북마크 조회",
            description = "사용자가 북마크한 몬스터들을 조회합니다. 다양한 필터링과 정렬 옵션을 제공합니다.\n\n" +
                    "**필터링 옵션:**\n" +
                    "- `minLevel`, `maxLevel`: 몬스터 레벨 범위\n\n" +
                    "**정렬 옵션:**\n" +
                    "- `createdAt`: 북마크 생성순 (최신순/오래된순)\n" +
                    "- `name`: 몬스터명 가나다순\n\n" +
                    "**정렬 사용 예시:**\n" +
                    "- `sort=createdAt,desc`: 최신 북마크순\n" +
                    "- `sort=name,asc`: 가나다순\n" +
                    "- 기본값: 최신 북마크순"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "몬스터 북마크 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Page<BookmarkSummaryDto>> getMonsterBookmarks(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ParameterObject MonsterBookmarkSearchRequestDto searchRequest,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        Page<BookmarkSummaryDto> monsterBookmarks = bookmarkQueryService.getMonsterBookmarks(
                principalDetails.getProviderId(), searchRequest, pageable);

        return ResponseEntity.ok(monsterBookmarks);
    }
}