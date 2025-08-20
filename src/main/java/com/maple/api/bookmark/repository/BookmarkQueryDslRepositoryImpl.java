package com.maple.api.bookmark.repository;

import com.maple.api.bookmark.application.dto.BookmarkSummaryDto;
import com.maple.api.bookmark.application.dto.ItemBookmarkSearchRequestDto;
import com.maple.api.bookmark.application.dto.MonsterBookmarkSearchRequestDto;
import com.maple.api.bookmark.domain.BookmarkType;
import com.maple.api.job.domain.Job;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
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
import static com.maple.api.item.domain.QEquipmentItem.equipmentItem;
import static com.maple.api.item.domain.QItem.item;
import static com.maple.api.item.domain.QItemJob.itemJob;
import static com.maple.api.map.domain.QMap.map;
import static com.maple.api.monster.domain.QMonster.monster;
import static com.maple.api.npc.domain.QNpc.npc;
import static com.maple.api.quest.domain.QQuest.quest;
import static com.maple.api.search.domain.QVwSearchSummary.vwSearchSummary;

@Repository
@RequiredArgsConstructor
public class BookmarkQueryDslRepositoryImpl implements BookmarkQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 북마크 전체 조회
     * @param memberId 멤버 ID
     * @param pageable 페이징, 정렬
     * @return 북마크 응답 DTO
     */
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
        Map<String, Path<?>> sortableProperties = Map.of(
                "name", vwSearchSummary.name,
                "createdAt", bookmark.createdAt
        );

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
                        bookmark.bookmarkId,
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

    /**
     * 북마크 아이템 조회
     * @param memberId 멤버 ID
     * @param request 필터링 데이터
     * @param pageable 페이징, 정렬
     * @return 북마크 응답 DTO
     */
    @Override
    public Page<BookmarkSummaryDto> searchItemBookmarks(String memberId, ItemBookmarkSearchRequestDto request, Pageable pageable) {
        BooleanBuilder whereClause = createItemBookmarkWhereClause(memberId, request);
        List<OrderSpecifier<?>> orderClause = createItemOrderClause(pageable);

        List<Integer> bookmarkIds = fetchItemBookmarkIds(whereClause, orderClause, pageable);
        if (bookmarkIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<BookmarkSummaryDto> content = fetchItemContent(bookmarkIds, orderClause);
        JPAQuery<Long> countQuery = createItemBookmarkCountQuery(whereClause);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanBuilder createItemBookmarkWhereClause(String memberId, ItemBookmarkSearchRequestDto request) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(bookmark.memberId.eq(memberId))
                .and(bookmark.bookmarkType.eq(BookmarkType.ITEM));

        if (request.jobIds() != null && !request.jobIds().isEmpty()) {
            builder.and(
                    new BooleanBuilder()
                            .or(itemJob.jobId.in(request.jobIds()))
                            .or(itemJob.jobId.eq(Job.COMMON_JOB_ID))
            );
        }
        if (request.minLevel() != null) {
            builder.and(equipmentItem.requiredStats.level.goe(request.minLevel()));
        }
        if (request.maxLevel() != null) {
            builder.and(equipmentItem.requiredStats.level.loe(request.maxLevel()));
        }
        if (request.categoryIds() != null && !request.categoryIds().isEmpty()) {
            builder.and(item.categoryId.in(request.categoryIds()));
        }

        return builder;
    }

    private List<Integer> fetchItemBookmarkIds(BooleanBuilder where, List<OrderSpecifier<?>> order, Pageable pageable) {
        return queryFactory
                .select(bookmark.bookmarkId)
                .from(bookmark)
                .join(item).on(bookmark.resourceId.eq(item.itemId))
                .leftJoin(equipmentItem).on(item.itemId.eq(equipmentItem.itemId))
                .leftJoin(itemJob).on(item.itemId.eq(itemJob.itemId))
                .where(where)
                .groupBy(bookmark.bookmarkId)
                .orderBy(order.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private List<BookmarkSummaryDto> fetchItemContent(List<Integer> bookmarkIds, List<OrderSpecifier<?>> order) {
        return queryFactory
                .select(Projections.constructor(BookmarkSummaryDto.class,
                        bookmark.bookmarkId,
                        item.itemId,
                        item.nameKr,
                        item.itemImageUrl,
                        Expressions.constant("ITEM"),
                        equipmentItem.requiredStats.level))
                .from(bookmark)
                .join(item).on(bookmark.resourceId.eq(item.itemId))
                .leftJoin(equipmentItem).on(item.itemId.eq(equipmentItem.itemId))
                .where(bookmark.bookmarkId.in(bookmarkIds))
                .orderBy(order.toArray(new OrderSpecifier[0]))
                .fetch();
    }

    private JPAQuery<Long> createItemBookmarkCountQuery(BooleanBuilder where) {
        return queryFactory
                .select(bookmark.countDistinct())
                .from(bookmark)
                .join(item).on(bookmark.resourceId.eq(item.itemId))
                .leftJoin(equipmentItem).on(item.itemId.eq(equipmentItem.itemId))
                .leftJoin(itemJob).on(item.itemId.eq(itemJob.itemId))
                .where(where);
    }

    private List<OrderSpecifier<?>> createItemOrderClause(Pageable pageable) {
        Map<String, Path<?>> sortableProperties = Map.of(
                "name", item.nameKr,
                "createdAt", bookmark.createdAt
        );

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

    /**
     * 북마크 몬스터 조회
     * @param memberId 멤버 ID
     * @param request 몬스터 필터링 데이터
     * @param pageable 페이징 데이터
     * @return 북마크 응답 DTO
     */
    @Override
    public Page<BookmarkSummaryDto> searchMonsterBookmarks(String memberId, MonsterBookmarkSearchRequestDto request, Pageable pageable) {
        BooleanBuilder whereClause = createMonsterBookmarkWhereClause(memberId, request);
        List<OrderSpecifier<?>> orderClause = createMonsterOrderClause(pageable);

        List<Integer> bookmarkIds = fetchMonsterBookmarkIds(whereClause, orderClause, pageable);
        if (bookmarkIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<BookmarkSummaryDto> content = fetchMonsterContent(bookmarkIds, orderClause);
        JPAQuery<Long> countQuery = createMonsterBookmarkCountQuery(whereClause);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanBuilder createMonsterBookmarkWhereClause(String memberId, MonsterBookmarkSearchRequestDto request) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(bookmark.memberId.eq(memberId))
                .and(bookmark.bookmarkType.eq(BookmarkType.MONSTER));

        if (request.minLevel() != null) {
            builder.and(monster.level.goe(request.minLevel()));
        }
        if (request.maxLevel() != null) {
            builder.and(monster.level.loe(request.maxLevel()));
        }

        return builder;
    }

    private List<Integer> fetchMonsterBookmarkIds(BooleanBuilder where, List<OrderSpecifier<?>> order, Pageable pageable) {
        return queryFactory
                .select(bookmark.bookmarkId)
                .from(bookmark)
                .join(monster).on(bookmark.resourceId.eq(monster.monsterId))
                .where(where)
                .orderBy(order.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private List<BookmarkSummaryDto> fetchMonsterContent(List<Integer> bookmarkIds, List<OrderSpecifier<?>> order) {
        return queryFactory
                .select(Projections.constructor(BookmarkSummaryDto.class,
                        bookmark.bookmarkId,
                        monster.monsterId,
                        monster.nameKr,
                        monster.imageUrl,
                        Expressions.constant("MONSTER"),
                        monster.level))
                .from(bookmark)
                .join(monster).on(bookmark.resourceId.eq(monster.monsterId))
                .where(bookmark.bookmarkId.in(bookmarkIds))
                .orderBy(order.toArray(new OrderSpecifier[0]))
                .fetch();
    }

    private JPAQuery<Long> createMonsterBookmarkCountQuery(BooleanBuilder where) {
        return queryFactory
                .select(bookmark.count())
                .from(bookmark)
                .join(monster).on(bookmark.resourceId.eq(monster.monsterId))
                .where(where);
    }

    private List<OrderSpecifier<?>> createMonsterOrderClause(Pageable pageable) {
        Map<String, Path<?>> sortableProperties = Map.of(
                "name", monster.nameKr,
                "createdAt", bookmark.createdAt
        );

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

    /**
     * 북마크 맵 조회
     * @param memberId 멤버 ID
     * @param pageable 페이징 데이터
     * @return 북마크 응답 DTO
     */
    @Override
    public Page<BookmarkSummaryDto> searchMapBookmarks(String memberId, Pageable pageable) {
        BooleanBuilder whereClause = createMapBookmarkWhereClause(memberId);
        List<OrderSpecifier<?>> orderClause = createMapOrderClause(pageable);

        List<Integer> bookmarkIds = fetchMapBookmarkIds(whereClause, orderClause, pageable);
        if (bookmarkIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<BookmarkSummaryDto> content = fetchMapContent(bookmarkIds, orderClause);
        JPAQuery<Long> countQuery = createMapBookmarkCountQuery(whereClause);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanBuilder createMapBookmarkWhereClause(String memberId) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(bookmark.memberId.eq(memberId))
                .and(bookmark.bookmarkType.eq(BookmarkType.MAP));

        return builder;
    }

    private List<Integer> fetchMapBookmarkIds(BooleanBuilder where, List<OrderSpecifier<?>> order, Pageable pageable) {
        return queryFactory
                .select(bookmark.bookmarkId)
                .from(bookmark)
                .join(map).on(bookmark.resourceId.eq(map.mapId))
                .where(where)
                .orderBy(order.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private List<BookmarkSummaryDto> fetchMapContent(List<Integer> bookmarkIds, List<OrderSpecifier<?>> order) {
        return queryFactory
                .select(Projections.constructor(BookmarkSummaryDto.class,
                        bookmark.bookmarkId,
                        map.mapId,
                        map.nameKr,
                        map.iconUrl,
                        Expressions.constant("MAP"),
                        Expressions.nullExpression(Integer.class)))
                .from(bookmark)
                .join(map).on(bookmark.resourceId.eq(map.mapId))
                .where(bookmark.bookmarkId.in(bookmarkIds))
                .orderBy(order.toArray(new OrderSpecifier[0]))
                .fetch();
    }

    private JPAQuery<Long> createMapBookmarkCountQuery(BooleanBuilder where) {
        return queryFactory
                .select(bookmark.count())
                .from(bookmark)
                .join(map).on(bookmark.resourceId.eq(map.mapId))
                .where(where);
    }

    private List<OrderSpecifier<?>> createMapOrderClause(Pageable pageable) {
        Map<String, Path<?>> sortableProperties = Map.of(
                "name", map.nameKr,
                "createdAt", bookmark.createdAt
        );

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

    /**
     * 북마크 NPC 조회
     * @param memberId 멤버 ID
     * @param pageable 페이징 데이터
     * @return 북마크 응답 DTO
     */
    @Override
    public Page<BookmarkSummaryDto> searchNpcBookmarks(String memberId, Pageable pageable) {
        BooleanBuilder whereClause = createNpcBookmarkWhereClause(memberId);
        List<OrderSpecifier<?>> orderClause = createNpcOrderClause(pageable);

        List<Integer> bookmarkIds = fetchNpcBookmarkIds(whereClause, orderClause, pageable);
        if (bookmarkIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<BookmarkSummaryDto> content = fetchNpcContent(bookmarkIds, orderClause);
        JPAQuery<Long> countQuery = createNpcBookmarkCountQuery(whereClause);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanBuilder createNpcBookmarkWhereClause(String memberId) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(bookmark.memberId.eq(memberId))
                .and(bookmark.bookmarkType.eq(BookmarkType.NPC));

        return builder;
    }

    private List<Integer> fetchNpcBookmarkIds(BooleanBuilder where, List<OrderSpecifier<?>> order, Pageable pageable) {
        return queryFactory
                .select(bookmark.bookmarkId)
                .from(bookmark)
                .join(npc).on(bookmark.resourceId.eq(npc.npcId))
                .where(where)
                .orderBy(order.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private List<BookmarkSummaryDto> fetchNpcContent(List<Integer> bookmarkIds, List<OrderSpecifier<?>> order) {
        return queryFactory
                .select(Projections.constructor(BookmarkSummaryDto.class,
                        bookmark.bookmarkId,
                        npc.npcId,
                        npc.nameKr,
                        npc.iconUrlDetail,
                        Expressions.constant("NPC"),
                        Expressions.nullExpression(Integer.class)))
                .from(bookmark)
                .join(npc).on(bookmark.resourceId.eq(npc.npcId))
                .where(bookmark.bookmarkId.in(bookmarkIds))
                .orderBy(order.toArray(new OrderSpecifier[0]))
                .fetch();
    }

    private JPAQuery<Long> createNpcBookmarkCountQuery(BooleanBuilder where) {
        return queryFactory
                .select(bookmark.count())
                .from(bookmark)
                .join(npc).on(bookmark.resourceId.eq(npc.npcId))
                .where(where);
    }

    private List<OrderSpecifier<?>> createNpcOrderClause(Pageable pageable) {
        Map<String, Path<?>> sortableProperties = Map.of(
                "name", npc.nameKr,
                "createdAt", bookmark.createdAt
        );

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

    /**
     * 북마크 퀘스트 조회
     * @param memberId 멤버 ID
     * @param pageable 페이징 데이터
     * @return 북마크 응답 DTO
     */
    @Override
    public Page<BookmarkSummaryDto> searchQuestBookmarks(String memberId, Pageable pageable) {
        BooleanBuilder whereClause = createQuestBookmarkWhereClause(memberId);
        List<OrderSpecifier<?>> orderClause = createQuestOrderClause(pageable);

        List<Integer> bookmarkIds = fetchQuestBookmarkIds(whereClause, orderClause, pageable);
        if (bookmarkIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<BookmarkSummaryDto> content = fetchQuestContent(bookmarkIds, orderClause);
        JPAQuery<Long> countQuery = createQuestBookmarkCountQuery(whereClause);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanBuilder createQuestBookmarkWhereClause(String memberId) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(bookmark.memberId.eq(memberId))
                .and(bookmark.bookmarkType.eq(BookmarkType.QUEST));

        return builder;
    }

    private List<Integer> fetchQuestBookmarkIds(BooleanBuilder where, List<OrderSpecifier<?>> order, Pageable pageable) {
        return queryFactory
                .select(bookmark.bookmarkId)
                .from(bookmark)
                .join(quest).on(bookmark.resourceId.eq(quest.questId))
                .where(where)
                .orderBy(order.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private List<BookmarkSummaryDto> fetchQuestContent(List<Integer> bookmarkIds, List<OrderSpecifier<?>> order) {
        return queryFactory
                .select(Projections.constructor(BookmarkSummaryDto.class,
                        bookmark.bookmarkId,
                        quest.questId,
                        quest.nameKr,
                        quest.iconUrl,
                        Expressions.constant("QUEST"),
                        Expressions.nullExpression(Integer.class)))
                .from(bookmark)
                .join(quest).on(bookmark.resourceId.eq(quest.questId))
                .where(bookmark.bookmarkId.in(bookmarkIds))
                .orderBy(order.toArray(new OrderSpecifier[0]))
                .fetch();
    }

    private JPAQuery<Long> createQuestBookmarkCountQuery(BooleanBuilder where) {
        return queryFactory
                .select(bookmark.count())
                .from(bookmark)
                .join(quest).on(bookmark.resourceId.eq(quest.questId))
                .where(where);
    }

    private List<OrderSpecifier<?>> createQuestOrderClause(Pageable pageable) {
        Map<String, Path<?>> sortableProperties = Map.of(
                "name", quest.nameKr,
                "createdAt", bookmark.createdAt
        );

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
}