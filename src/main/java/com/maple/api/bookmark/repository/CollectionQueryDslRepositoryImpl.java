package com.maple.api.bookmark.repository;

import com.maple.api.bookmark.application.dto.BookmarkSummaryDto;
import com.maple.api.bookmark.application.dto.CollectionWithBookmarksDto;
import com.maple.api.bookmark.domain.Collection;
import com.maple.api.bookmark.repository.BookmarkCollectionRepository.CollectionBookmarkRow;
import com.maple.api.bookmark.repository.BookmarkCollectionRepository;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.maple.api.bookmark.domain.QCollection.collection;

@Repository
@RequiredArgsConstructor
public class CollectionQueryDslRepositoryImpl implements CollectionQueryDslRepository {

    private final JPAQueryFactory queryFactory;
    private final BookmarkCollectionRepository bookmarkCollectionRepository;

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

        // 3단계: 윈도우 함수 기반 네이티브 조회로 컬렉션당 상위 4개만 일괄 로드
        List<CollectionBookmarkRow> rows = bookmarkCollectionRepository
                .findTopRecentBookmarksByCollections(memberId, collectionIds);

        // 4단계: 메모리에서 컬렉션별로 북마크를 조립 (네이티브에서 이미 정렬/상한 보장)
        Map<Integer, List<BookmarkSummaryDto>> bookmarksByCollectionId = new LinkedHashMap<>();
        for (CollectionBookmarkRow r : rows) {
            bookmarksByCollectionId
                    .computeIfAbsent(r.getCollectionId(), k -> new ArrayList<>(4))
                    .add(new BookmarkSummaryDto(
                            r.getBookmarkId(),
                            r.getOriginalId(),
                            r.getName(),
                            r.getImageUrl(),
                            r.getType(),
                            r.getLevel()
                    ));
        }

        // 5단계: 최종 DTO 조합
        List<CollectionWithBookmarksDto> content = collections.stream()
                .map(coll -> CollectionWithBookmarksDto.of(
                        coll,
                        bookmarksByCollectionId.getOrDefault(coll.getCollectionId(), new ArrayList<>())
                ))
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

    // 이전 QueryDSL 기반 조립/중복제거 로직은 네이티브 윈도우 함수로 대체되었습니다.

    private JPAQuery<Long> createCountQuery(String memberId) {
        return queryFactory
                .select(collection.count())
                .from(collection)
                .where(collection.memberId.eq(memberId));
    }
}
