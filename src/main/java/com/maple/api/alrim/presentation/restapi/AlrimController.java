package com.maple.api.alrim.presentation.restapi;

import com.maple.api.alrim.application.command.AlrimCommandService;
import com.maple.api.alrim.application.query.AlrimDTO;
import com.maple.api.alrim.application.query.AlrimDTOWithReadInfo;
import com.maple.api.alrim.application.query.AlrimQueryService;
import com.maple.api.alrim.common.CursorPage;
import com.maple.api.auth.domain.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
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
  @PostMapping("/read")
  public void readAlrim(
    @AuthenticationPrincipal PrincipalDetails principalDetails,
    String alrimLink
  ) {
    commandService.readAlrim(
      principalDetails.getProviderId(),
      alrimLink
    );
  }

  @GetMapping("/all")
  public CursorPage<AlrimDTOWithReadInfo> getAllAlrim(
    @AuthenticationPrincipal PrincipalDetails principalDetails,
    @Nullable LocalDateTime cursor,
    int pageSize
  ) {
    return queryService.findAllForAlrimForMemberWithCursor(
      principalDetails.getProviderId(),
      cursor,
      pageSize
    );
  }

  @GetMapping("/list/notices")
  public CursorPage<AlrimDTO> getAllNotices(
    @Nullable LocalDateTime cursor,
    int pageSize
  ) {
    return queryService.findAllNotices(
      cursor,
      pageSize
    );
  }

  @GetMapping("/list/patch-notes")
  public CursorPage<AlrimDTO> getAllPatchNotes(
    @Nullable LocalDateTime cursor,
    int pageSize
  ) {
    return queryService.findAllPatchNotes(
      cursor,
      pageSize
    );
  }

  @GetMapping("/list/events/ongoing")
  public CursorPage<AlrimDTO> getOngoingEvents(
    @Nullable LocalDateTime cursor,
    int pageSize
  ) {
    return queryService.findAllOnGoingEvents(
      cursor,
      pageSize
    );
  }

  @GetMapping("/list/events/outdated")
  public CursorPage<AlrimDTO> getOutdatedEvents(
    @Nullable LocalDateTime cursor,
    int pageSize
  ) {
    return queryService.findAllOutDatedEvents(
      cursor,
      pageSize
    );
  }
}
