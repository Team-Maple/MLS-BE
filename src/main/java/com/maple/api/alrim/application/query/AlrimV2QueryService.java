package com.maple.api.alrim.application.query;

import com.maple.api.alrim.common.CursorPage;
import com.maple.api.alrim.domain.Alrim;
import com.maple.api.alrim.domain.AlrimType;
import com.maple.api.alrim.repository.AlrimQueryRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlrimV2QueryService {
  private final AlrimQueryRepository alrimQueryRepository;

  public AlrimV2QueryService(AlrimQueryRepository alrimQueryRepository) {
    this.alrimQueryRepository = alrimQueryRepository;
  }

  @Transactional(readOnly = true)
  public CursorPage<AlrimV2DTO> findAllNotices(@Nullable Long cursor, int pageSize) {
    return toCursorPage(
      alrimQueryRepository.findAllAfterCursor(AlrimType.NOTICE, false, cursor, pageSize)
    );
  }

  @Transactional(readOnly = true)
  public CursorPage<AlrimV2DTO> findAllPatchNotes(@Nullable Long cursor, int pageSize) {
    return toCursorPage(
      alrimQueryRepository.findAllAfterCursor(AlrimType.PATCH_NOTE, false, cursor, pageSize)
    );
  }

  @Transactional(readOnly = true)
  public CursorPage<AlrimV2DTO> findAllOnGoingEvents(@Nullable Long cursor, int pageSize) {
    return toCursorPage(
      alrimQueryRepository.findAllAfterCursor(AlrimType.EVENT, false, cursor, pageSize)
    );
  }

  @Transactional(readOnly = true)
  public CursorPage<AlrimV2DTO> findAllOutDatedEvents(@Nullable Long cursor, int pageSize) {
    return toCursorPage(
      alrimQueryRepository.findAllAfterCursor(AlrimType.EVENT, true, cursor, pageSize)
    );
  }

  private CursorPage<AlrimV2DTO> toCursorPage(CursorPage<Alrim> alrimPage) {
    return new CursorPage<>(
      alrimPage.getContents().stream()
        .map(AlrimV2DTO::toDTO)
        .toList(),
      alrimPage.getHasMore()
    );
  }
}
