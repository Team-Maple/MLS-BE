package com.maple.api.bookmark.presentation.restapi;

import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.bookmark.application.CollectionService;
import com.maple.api.bookmark.application.dto.CollectionResponseDto;
import com.maple.api.bookmark.application.dto.CreateCollectionRequestDto;
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
}