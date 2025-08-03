package com.maple.api.alrim.application.query;

import com.maple.api.alrim.common.CursorPage;
import com.maple.api.alrim.domain.Alrim;
import com.maple.api.alrim.domain.AlrimRead;
import com.maple.api.alrim.domain.AlrimType;
import com.maple.api.alrim.repository.AlrimQueryRepository;
import com.maple.api.alrim.repository.AlrimReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlrimQueryService {
  private final AlrimQueryRepository alrimQueryRepository;
  private final AlrimReadRepository alrimReadRepository;

  // 3개월 이내의 모든 알림 확인
  @Transactional(readOnly = true)
  public CursorPage<AlrimDTOWithReadInfo> findAllForAlrimForMemberWithCursor(
    String memberId,
    @Nullable LocalDateTime cursor,
    int pageSize
  ) {
    CursorPage<Alrim> alrimPage = alrimQueryRepository.findAllAfterCursor(cursor, pageSize);

    val alreadyReadSet =
      alrimReadRepository.findAllByMemberIdAndAlrimLinkIn(
          memberId,
          alrimPage.getContents().stream().map(Alrim::getLink).toList()
        )
        .stream().map(AlrimRead::getAlrimLink)
        .collect(Collectors.toSet());

    return new CursorPage<>(
      alrimPage.getContents().stream()
        .map(it -> AlrimDTOWithReadInfo.toDTO(it, alreadyReadSet.contains(it.getLink())))
        .toList(),
      alrimPage.getHasMore()
    );
  }


  @Transactional(readOnly = true)
  public CursorPage<AlrimDTO> findAllNotices(
    @Nullable LocalDateTime cursor,
    int pageSize
  ) {
    CursorPage<Alrim> alrimPage = alrimQueryRepository.findAllAfterCursor(
      AlrimType.NOTICE, false, cursor, pageSize
    );

    return new CursorPage<>(
      alrimPage.getContents().stream()
        .map(AlrimDTO::toDTO)
        .toList(),
      alrimPage.getHasMore()
    );
  }

  @Transactional(readOnly = true)
  public CursorPage<AlrimDTO> findAllPatchNotes(
    @Nullable LocalDateTime cursor,
    int pageSize
  ) {
    CursorPage<Alrim> alrimPage = alrimQueryRepository.findAllAfterCursor(
      AlrimType.PATCH_NOTE, false, cursor, pageSize
    );

    return new CursorPage<>(
      alrimPage.getContents().stream()
        .map(AlrimDTO::toDTO)
        .toList(),
      alrimPage.getHasMore()
    );
  }

  @Transactional(readOnly = true)
  public CursorPage<AlrimDTO> findAllOnGoingEvents(
    @Nullable LocalDateTime cursor,
    int pageSize
  ) {
    CursorPage<Alrim> alrimPage = alrimQueryRepository.findAllAfterCursor(
      AlrimType.EVENT, false, cursor, pageSize
    );

    return new CursorPage<>(
      alrimPage.getContents().stream()
        .map(AlrimDTO::toDTO)
        .toList(),
      alrimPage.getHasMore()
    );
  }

  @Transactional(readOnly = true)
  public CursorPage<AlrimDTO> findAllOutDatedEvents(
    @Nullable LocalDateTime cursor,
    int pageSize
  ) {
    CursorPage<Alrim> alrimPage = alrimQueryRepository.findAllAfterCursor(
      AlrimType.EVENT, true, cursor, pageSize
    );

    return new CursorPage<>(
      alrimPage.getContents().stream()
        .map(AlrimDTO::toDTO)
        .toList(),
      alrimPage.getHasMore()
    );
  }
}
