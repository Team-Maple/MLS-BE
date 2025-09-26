package com.maple.api.alrim.presentation.restapi;

import com.maple.api.alrim.application.command.AlrimCommandService;
import com.maple.api.alrim.application.query.AlrimDTO;
import com.maple.api.alrim.application.query.AlrimDTOWithReadInfo;
import com.maple.api.alrim.application.query.AlrimQueryService;
import com.maple.api.alrim.common.CursorPage;
import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.common.presentation.restapi.ResponseTemplate;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/alrim")
public class AlrimController {
  private final AlrimCommandService commandService;
  private final AlrimQueryService queryService;

  @Operation(
    summary = "알림 읽기 처리",
    description = "각 멤버의 알림 읽음 처리"
  )
  @PostMapping("/set-read")
  public ResponseEntity<ResponseTemplate<Void>> setReadAlrim(
    @AuthenticationPrincipal PrincipalDetails principalDetails,
    String alrimLink
  ) {
    commandService.setReadAlrim(
      principalDetails.getProviderId(),
      alrimLink
    );
    return ResponseEntity.ok(ResponseTemplate.success(null));
  }

  @GetMapping("/all")
  public ResponseEntity<ResponseTemplate<CursorPage<AlrimDTOWithReadInfo>>> getAllAlrim(
    @AuthenticationPrincipal PrincipalDetails principalDetails,
    @Nullable LocalDateTime cursor,
    int pageSize
  ) {
    CursorPage<AlrimDTOWithReadInfo> result = queryService.findAllForAlrimForMemberWithCursor(
      principalDetails.getProviderId(),
      cursor,
      pageSize
    );
    return ResponseEntity.ok(ResponseTemplate.success(result));
  }

  @GetMapping("/list/notices")
  public ResponseEntity<ResponseTemplate<CursorPage<AlrimDTO>>> getAllNotices(
    @Nullable LocalDateTime cursor,
    int pageSize
  ) {
    CursorPage<AlrimDTO> result = queryService.findAllNotices(
      cursor,
      pageSize
    );
    return ResponseEntity.ok(ResponseTemplate.success(result));
  }

  @GetMapping("/list/patch-notes")
  public ResponseEntity<ResponseTemplate<CursorPage<AlrimDTO>>> getAllPatchNotes(
    @Nullable LocalDateTime cursor,
    int pageSize
  ) {
    CursorPage<AlrimDTO> result = queryService.findAllPatchNotes(
      cursor,
      pageSize
    );
    return ResponseEntity.ok(ResponseTemplate.success(result));
  }

  @GetMapping("/list/events/ongoing")
  public ResponseEntity<ResponseTemplate<CursorPage<AlrimDTO>>> getOngoingEvents(
    @Nullable LocalDateTime cursor,
    int pageSize
  ) {
    CursorPage<AlrimDTO> result = queryService.findAllOnGoingEvents(
      cursor,
      pageSize
    );
    return ResponseEntity.ok(ResponseTemplate.success(result));
  }

  @GetMapping("/list/events/outdated")
  public ResponseEntity<ResponseTemplate<CursorPage<AlrimDTO>>> getOutdatedEvents(
    @Nullable LocalDateTime cursor,
    int pageSize
  ) {
    CursorPage<AlrimDTO> result = queryService.findAllOutDatedEvents(
      cursor,
      pageSize
    );
    return ResponseEntity.ok(ResponseTemplate.success(result));
  }
}
