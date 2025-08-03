package com.maple.api.alrim.repository;

import com.maple.api.alrim.common.CursorPage;
import com.maple.api.alrim.domain.Alrim;
import com.maple.api.alrim.domain.AlrimType;
import com.maple.api.alrim.domain.QAlrim;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class AlrimQueryRepository {
  private final JPAQueryFactory queryFactory;

  private final QAlrim alrim = QAlrim.alrim;

  public CursorPage<Alrim> findAllAfterCursor(
    @Nullable LocalDateTime cursor,
    int pageSize
  ) {

    BooleanBuilder builder = new BooleanBuilder();
    if (cursor != null) {
      builder.and(alrim.date.lt(cursor));
    }

    val results = queryFactory.selectFrom(alrim)
      .where(builder)
      .orderBy(alrim.date.desc())
      .limit(pageSize + 1)
      .fetch();

    boolean hasMore = results.size() > pageSize;
    return new CursorPage<>(hasMore ? results.subList(0, pageSize) : results, hasMore);
  }

  public CursorPage<Alrim> findAllAfterCursor(
    AlrimType alrimType,
    boolean isOutdated,
    @Nullable LocalDateTime cursor,
    int pageSize
  ) {
    BooleanBuilder builder = new BooleanBuilder(
      alrim.type.eq(alrimType)
        .and(alrim.outdated.eq(isOutdated))
    );

    if (cursor != null) {
      builder.and(alrim.date.lt(cursor));
    }

    val results = queryFactory.selectFrom(alrim)
      .where(builder)
      .orderBy(alrim.date.desc())
      .limit(pageSize + 1)
      .fetch();

    boolean hasMore = results.size() > pageSize;
    return new CursorPage<>(hasMore ? results.subList(0, pageSize) : results, hasMore);
  }
}
