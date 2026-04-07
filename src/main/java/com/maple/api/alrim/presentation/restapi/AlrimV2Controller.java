package com.maple.api.alrim.presentation.restapi;

import com.maple.api.alrim.application.query.AlrimV2DTO;
import com.maple.api.alrim.application.query.AlrimV2DTOWithReadInfo;
import com.maple.api.alrim.application.query.AlrimV2QueryService;
import com.maple.api.alrim.common.CursorPage;
import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.common.presentation.restapi.ResponseTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/alrim")
public class AlrimV2Controller {
  private final AlrimV2QueryService queryService;

  public AlrimV2Controller(AlrimV2QueryService queryService) {
    this.queryService = queryService;
  }

  @GetMapping("/all")
  public ResponseEntity<ResponseTemplate<CursorPage<AlrimV2DTOWithReadInfo>>> getAllAlrim(
    @AuthenticationPrincipal PrincipalDetails principalDetails,
    @Nullable Long cursor,
    int pageSize
  ) {
    CursorPage<AlrimV2DTOWithReadInfo> result = queryService.findAllForAlrimForMemberWithCursor(
      principalDetails.getProviderId(),
      cursor,
      pageSize
    );
    return ResponseEntity.ok(ResponseTemplate.success(result));
  }

  @GetMapping("/list/notices")
  public ResponseEntity<ResponseTemplate<CursorPage<AlrimV2DTO>>> getAllNotices(
    @Nullable Long cursor,
    int pageSize
  ) {
    CursorPage<AlrimV2DTO> result = queryService.findAllNotices(cursor, pageSize);
    return ResponseEntity.ok(ResponseTemplate.success(result));
  }

  @GetMapping("/list/patch-notes")
  public ResponseEntity<ResponseTemplate<CursorPage<AlrimV2DTO>>> getAllPatchNotes(
    @Nullable Long cursor,
    int pageSize
  ) {
    CursorPage<AlrimV2DTO> result = queryService.findAllPatchNotes(cursor, pageSize);
    return ResponseEntity.ok(ResponseTemplate.success(result));
  }

  @GetMapping("/list/events/ongoing")
  public ResponseEntity<ResponseTemplate<CursorPage<AlrimV2DTO>>> getOngoingEvents(
    @Nullable Long cursor,
    int pageSize
  ) {
    CursorPage<AlrimV2DTO> result = queryService.findAllOnGoingEvents(cursor, pageSize);
    return ResponseEntity.ok(ResponseTemplate.success(result));
  }

  @GetMapping("/list/events/outdated")
  public ResponseEntity<ResponseTemplate<CursorPage<AlrimV2DTO>>> getOutdatedEvents(
    @Nullable Long cursor,
    int pageSize
  ) {
    CursorPage<AlrimV2DTO> result = queryService.findAllOutDatedEvents(cursor, pageSize);
    return ResponseEntity.ok(ResponseTemplate.success(result));
  }
}
