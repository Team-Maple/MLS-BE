package com.maple.api.bookmark.repository;

import com.maple.api.bookmark.application.dto.BookmarkSummaryDto;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.maple.api.bookmark.domain.QBookmark.bookmark;
import static com.maple.api.search.domain.QVwSearchSummary.vwSearchSummary;

@Repository
@RequiredArgsConstructor
public class BookmarkQueryDslRepositoryImpl implements BookmarkQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    private final Map<String, Path<?>> sortableProperties = Map.of(
            "name", vwSearchSummary.name,
            "createdAt", bookmark.createdAt
    );

    @Override
    public Page<BookmarkSummaryDto> searchBookmarks(String memberId, Pageable pageable) {
        // 1. ORDER BY 절 생성
        List<OrderSpecifier<?>> orderClause = createOrderClause(pageable);

        // 2. 최적화된 ID 목록 조회
        List<Integer> bookmarkIds = fetchBookmarkIds(memberId, orderClause, pageable);
        if (bookmarkIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // 3. ID 목록으로 실제 컨텐츠 조회
        List<BookmarkSummaryDto> content = fetchContent(bookmarkIds, orderClause);

        // 4. 전체 카운트 쿼리 생성
        JPAQuery<Long> countQuery = createCountQuery(memberId);

        // 5. Page 객체로 변환하여 반환
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private List<OrderSpecifier<?>> createOrderClause(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return List.of(bookmark.createdAt.desc());
        }

        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        pageable.getSort().forEach(order -> {
            Path<?> path = sortableProperties.get(order.getProperty());
            if (path != null) {
                orderSpecifiers.add(new OrderSpecifier(order.isAscending() ? Order.ASC : Order.DESC, path));
            }
        });

        if (orderSpecifiers.isEmpty()) {
            orderSpecifiers.add(bookmark.createdAt.desc());
        }

        return orderSpecifiers;
    }

    private List<Integer> fetchBookmarkIds(String memberId, List<OrderSpecifier<?>> order, Pageable pageable) {
        return queryFactory
                .select(bookmark.bookmarkId)
                .from(bookmark)
                .join(vwSearchSummary).on(
                        bookmark.resourceId.eq(vwSearchSummary.originalId)
                                .and(bookmark.bookmarkType.stringValue().eq(vwSearchSummary.type))
                )
                .where(bookmark.memberId.eq(memberId))
                .orderBy(order.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private List<BookmarkSummaryDto> fetchContent(List<Integer> bookmarkIds, List<OrderSpecifier<?>> order) {
        return queryFactory
                .select(Projections.constructor(BookmarkSummaryDto.class,
                        vwSearchSummary.originalId,
                        vwSearchSummary.name,
                        vwSearchSummary.imageUrl,
                        vwSearchSummary.type,
                        vwSearchSummary.level
                ))
                .from(bookmark)
                .join(vwSearchSummary).on(
                        bookmark.resourceId.eq(vwSearchSummary.originalId)
                                .and(bookmark.bookmarkType.stringValue().eq(vwSearchSummary.type))
                )
                .where(bookmark.bookmarkId.in(bookmarkIds))
                .orderBy(order.toArray(new OrderSpecifier[0]))
                .fetch();
    }

    private JPAQuery<Long> createCountQuery(String memberId) {
        return queryFactory
                .select(bookmark.count())
                .from(bookmark)
                .where(bookmark.memberId.eq(memberId));
    }
}