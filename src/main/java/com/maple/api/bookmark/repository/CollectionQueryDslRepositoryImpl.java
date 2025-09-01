package com.maple.api.bookmark.repository;

import com.maple.api.bookmark.application.dto.BookmarkSummaryDto;
import com.maple.api.bookmark.application.dto.CollectionWithBookmarksDto;
import com.maple.api.bookmark.domain.Collection;
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
import java.util.stream.Collectors;

import static com.maple.api.bookmark.domain.QBookmark.bookmark;
import static com.maple.api.bookmark.domain.QBookmarkCollection.bookmarkCollection;
import static com.maple.api.bookmark.domain.QCollection.collection;
import static com.maple.api.search.domain.QVwSearchSummary.vwSearchSummary;

@Repository
@RequiredArgsConstructor
public class CollectionQueryDslRepositoryImpl implements CollectionQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<CollectionWithBookmarksDto> findCollectionsWithRecentBookmarks(String memberId, Pageable pageable) {
        // 1단계: 페이징된 컬렉션 ID 조회
        List<OrderSpecifier<?>> orderSpecifiers = createOrderClause(pageable);
        List<Integer> collectionIds = fetchCollectionIds(memberId, orderSpecifiers, pageable);
        
        if (collectionIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // 2단계: 컬렉션 상세 정보 조회
        List<Collection> collections = fetchCollectionDetails(collectionIds, orderSpecifiers);

        // 3단계: 해당 컬렉션들의 북마크 일괄 조회
        List<BookmarkSummaryDto> allBookmarks = fetchRecentBookmarksByCollections(memberId, collectionIds);

        // 4단계: 메모리에서 컬렉션별로 북마크 그룹핑 및 제한
        Map<Integer, List<BookmarkSummaryDto>> bookmarksByCollectionId = allBookmarks.stream()
                .collect(Collectors.groupingBy(
                        bookmark -> findCollectionIdByBookmarkId(bookmark.bookmarkId(), collectionIds),
                        Collectors.toList()
                ));

        // 5단계: 최종 DTO 조합
        List<CollectionWithBookmarksDto> content = collections.stream()
                .map(coll -> {
                    List<BookmarkSummaryDto> recentBookmarks = bookmarksByCollectionId
                            .getOrDefault(coll.getCollectionId(), new ArrayList<>())
                            .stream()
                            .limit(4)
                            .collect(Collectors.toList());
                    
                    return CollectionWithBookmarksDto.of(coll, recentBookmarks);
                })
                .collect(Collectors.toList());

        // 6단계: 전체 카운트 쿼리
        JPAQuery<Long> countQuery = createCountQuery(memberId);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private List<OrderSpecifier<?>> createOrderClause(Pageable pageable) {
        Map<String, Path<?>> sortableProperties = Map.of(
                "name", collection.name,
                "createdAt", collection.createdAt
        );

        if (pageable.getSort().isUnsorted()) {
            return List.of(collection.createdAt.desc());
        }

        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        pageable.getSort().forEach(order -> {
            Path<?> path = sortableProperties.get(order.getProperty());
            if (path != null) {
                @SuppressWarnings("unchecked")
                OrderSpecifier<?> orderSpecifier = new OrderSpecifier(order.isAscending() ? Order.ASC : Order.DESC, path);
                orderSpecifiers.add(orderSpecifier);
            }
        });

        if (orderSpecifiers.isEmpty()) {
            orderSpecifiers.add(collection.createdAt.desc());
        }

        return orderSpecifiers;
    }

    private List<Integer> fetchCollectionIds(String memberId, List<OrderSpecifier<?>> orderSpecifiers, Pageable pageable) {
        return queryFactory
                .select(collection.collectionId)
                .from(collection)
                .where(collection.memberId.eq(memberId))
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private List<Collection> fetchCollectionDetails(List<Integer> collectionIds, List<OrderSpecifier<?>> orderSpecifiers) {
        return queryFactory
                .selectFrom(collection)
                .where(collection.collectionId.in(collectionIds))
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .fetch();
    }

    private List<BookmarkSummaryDto> fetchRecentBookmarksByCollections(String memberId, List<Integer> collectionIds) {
        return queryFactory
                .select(Projections.constructor(BookmarkSummaryDto.class,
                        bookmark.bookmarkId,
                        vwSearchSummary.originalId,
                        vwSearchSummary.name,
                        vwSearchSummary.imageUrl,
                        vwSearchSummary.type,
                        vwSearchSummary.level
                ))
                .from(bookmarkCollection)
                .join(bookmark).on(bookmarkCollection.bookmarkId.eq(bookmark.bookmarkId))
                .join(vwSearchSummary).on(
                        bookmark.resourceId.eq(vwSearchSummary.originalId)
                                .and(bookmark.bookmarkType.stringValue().eq(vwSearchSummary.type))
                )
                .where(bookmarkCollection.collectionId.in(collectionIds)
                        .and(bookmark.memberId.eq(memberId)))
                .orderBy(bookmarkCollection.collectionId.asc(), bookmark.createdAt.desc())
                .fetch();
    }

    private Integer findCollectionIdByBookmarkId(Integer bookmarkId, List<Integer> collectionIds) {
        return queryFactory
                .select(bookmarkCollection.collectionId)
                .from(bookmarkCollection)
                .where(bookmarkCollection.bookmarkId.eq(bookmarkId)
                        .and(bookmarkCollection.collectionId.in(collectionIds)))
                .fetchFirst();
    }

    private JPAQuery<Long> createCountQuery(String memberId) {
        return queryFactory
                .select(collection.count())
                .from(collection)
                .where(collection.memberId.eq(memberId));
    }
}