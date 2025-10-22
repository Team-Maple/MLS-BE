package com.maple.api.bookmark.presentation.restapi;

import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.bookmark.application.BookmarkQueryService;
import com.maple.api.bookmark.application.BookmarkService;
import com.maple.api.bookmark.application.CollectionService;
import com.maple.api.bookmark.application.dto.*;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
                    "**정렬 옵션:**\n" +
                    "- `createdAt`: 북마크 생성순 정렬 (최신순 기본값)\n" +
                    "- `name`: 이름순 정렬\n\n" +
                    "**정렬 사용 예시:**\n" +
                    "- `sort=createdAt,desc`: 최신 북마크순\n" +
                    "- `sort=name,asc`: 이름 오름차순"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "북마크 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseTemplate<List<BookmarkSummaryDto>>> getBookmarks(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @ParameterObject Sort sort) {

        Sort effectiveSort = resolveSort(sort);
        Page<BookmarkSummaryDto> bookmarks = bookmarkQueryService.getBookmarks(
                principalDetails.getProviderId(), PageRequest.of(0, Integer.MAX_VALUE, effectiveSort));

        return ResponseEntity.ok(ResponseTemplate.success(bookmarks.getContent()));
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
    public ResponseEntity<ResponseTemplate<BookmarkResponseDto>> createBookmark(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody CreateBookmarkRequestDto request) {

        BookmarkResponseDto response = bookmarkService.createBookmark(
                principalDetails.getProviderId(), request);

        return ResponseEntity.ok(ResponseTemplate.success(response));
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
    public ResponseEntity<ResponseTemplate<BookmarkAddToCollectionsResponseDto>> addBookmarkToCollections(
            @Parameter(description = "북마크 ID") @PathVariable Integer bookmarkId,
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody BookmarkAddToCollectionsRequestDto request) {

        BookmarkAddToCollectionsResponseDto response = collectionService.addBookmarkToCollections(
                principalDetails.getProviderId(), bookmarkId, request);

        return ResponseEntity.ok(ResponseTemplate.success(response));
    }

    @DeleteMapping("/{bookmarkId}")
    @Operation(
            summary = "북마크 삭제",
            description = "특정 북마크를 삭제합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "북마크 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "북마크를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseTemplate<Void>> deleteBookmark(
            @Parameter(description = "북마크 ID") @PathVariable Integer bookmarkId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        bookmarkService.deleteBookmark(principalDetails.getProviderId(), bookmarkId);

        return ResponseEntity.ok(ResponseTemplate.success(null));
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
    public ResponseEntity<ResponseTemplate<List<BookmarkSummaryDto>>> getItemBookmarks(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ParameterObject ItemBookmarkSearchRequestDto searchRequest,
            @ParameterObject Sort sort) {

        Sort effectiveSort = resolveSort(sort);
        Page<BookmarkSummaryDto> itemBookmarks = bookmarkQueryService.getItemBookmarks(
                principalDetails.getProviderId(), searchRequest, PageRequest.of(0, Integer.MAX_VALUE, effectiveSort));

        return ResponseEntity.ok(ResponseTemplate.success(itemBookmarks.getContent()));
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
    public ResponseEntity<ResponseTemplate<List<BookmarkSummaryDto>>> getMonsterBookmarks(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ParameterObject MonsterBookmarkSearchRequestDto searchRequest,
            @ParameterObject Sort sort) {

        Sort effectiveSort = resolveSort(sort);
        Page<BookmarkSummaryDto> monsterBookmarks = bookmarkQueryService.getMonsterBookmarks(
                principalDetails.getProviderId(), searchRequest, PageRequest.of(0, Integer.MAX_VALUE, effectiveSort));

        return ResponseEntity.ok(ResponseTemplate.success(monsterBookmarks.getContent()));
    }

    @GetMapping("/maps")
    @Operation(
            summary = "맵 북마크 조회",
            description = "사용자가 북마크한 맵들을 조회합니다. 정렬 옵션을 제공합니다.\n\n" +
                    "**정렬 옵션:**\n" +
                    "- `createdAt`: 북마크 생성순 (최신순/오래된순)\n" +
                    "- `name`: 맵명 가나다순\n\n" +
                    "**정렬 사용 예시:**\n" +
                    "- `sort=createdAt,desc`: 최신 북마크순\n" +
                    "- `sort=name,asc`: 가나다순\n" +
                    "- 기본값: 최신 북마크순"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "맵 북마크 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseTemplate<List<BookmarkSummaryDto>>> getMapBookmarks(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @ParameterObject Sort sort) {

        Sort effectiveSort = resolveSort(sort);
        Page<BookmarkSummaryDto> mapBookmarks = bookmarkQueryService.getMapBookmarks(
                principalDetails.getProviderId(), PageRequest.of(0, Integer.MAX_VALUE, effectiveSort));

        return ResponseEntity.ok(ResponseTemplate.success(mapBookmarks.getContent()));
    }

    @GetMapping("/npcs")
    @Operation(
            summary = "NPC 북마크 조회",
            description = "사용자가 북마크한 NPC들을 조회합니다. 정렬 옵션을 제공합니다.\n\n" +
                    "**정렬 옵션:**\n" +
                    "- `createdAt`: 북마크 생성순 (최신순/오래된순)\n" +
                    "- `name`: NPC명 가나다순\n\n" +
                    "**정렬 사용 예시:**\n" +
                    "- `sort=createdAt,desc`: 최신 북마크순\n" +
                    "- `sort=name,asc`: 가나다순\n" +
                    "- 기본값: 최신 북마크순"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "NPC 북마크 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseTemplate<List<BookmarkSummaryDto>>> getNpcBookmarks(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @ParameterObject Sort sort) {

        Sort effectiveSort = resolveSort(sort);
        Page<BookmarkSummaryDto> npcBookmarks = bookmarkQueryService.getNpcBookmarks(
                principalDetails.getProviderId(), PageRequest.of(0, Integer.MAX_VALUE, effectiveSort));

        return ResponseEntity.ok(ResponseTemplate.success(npcBookmarks.getContent()));
    }

    @GetMapping("/quests")
    @Operation(
            summary = "퀘스트 북마크 조회",
            description = "사용자가 북마크한 퀘스트들을 조회합니다. 정렬 옵션을 제공합니다.\n\n" +
                    "**정렬 옵션:**\n" +
                    "- `createdAt`: 북마크 생성순 (최신순/오래된순)\n" +
                    "- `name`: 퀘스트명 가나다순\n\n" +
                    "**정렬 사용 예시:**\n" +
                    "- `sort=createdAt,desc`: 최신 북마크순\n" +
                    "- `sort=name,asc`: 가나다순\n" +
                    "- 기본값: 최신 북마크순"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "퀘스트 북마크 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseTemplate<List<BookmarkSummaryDto>>> getQuestBookmarks(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @ParameterObject Sort sort) {

        Sort effectiveSort = resolveSort(sort);
        Page<BookmarkSummaryDto> questBookmarks = bookmarkQueryService.getQuestBookmarks(
                principalDetails.getProviderId(), PageRequest.of(0, Integer.MAX_VALUE, effectiveSort));

        return ResponseEntity.ok(ResponseTemplate.success(questBookmarks.getContent()));
    }

    private Sort resolveSort(Sort requestedSort) {
        if (requestedSort != null && requestedSort.isSorted()) {
            return requestedSort;
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
