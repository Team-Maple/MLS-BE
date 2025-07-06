package com.maple.api.item.repository;

import com.maple.api.item.application.dto.ItemSearchRequestDto;
import com.maple.api.item.domain.Item;
import com.maple.api.job.domain.Job;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.maple.api.item.domain.QEquipmentItem.equipmentItem;
import static com.maple.api.item.domain.QItem.item;
import static com.maple.api.item.domain.QItemJob.itemJob;

@Repository
@RequiredArgsConstructor
public class ItemQueryDslRepositoryImpl implements ItemQueryDslRepository {
    private final JPAQueryFactory queryFactory;
    
    private final Map<String, Path<?>> sortableProperties = Map.of(
            "name", item.nameKr,
            "level", equipmentItem.requiredStats.level,
            "itemId", item.itemId
    );

    @Override
    public Page<Item> searchItems(ItemSearchRequestDto searchRequest, Pageable pageable) {
        // 1. WHERE 절 생성
        BooleanBuilder whereClause = createWhereClause(searchRequest);

        // 2. ORDER BY 절 생성
        List<OrderSpecifier<?>> orderClause = createOrderClause(pageable);

        // 3. 최적화된 ID 목록 조회
        List<Integer> itemIds = fetchItemIds(whereClause, orderClause, pageable);
        if (itemIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // 4. ID 목록으로 실제 컨텐츠 조회
        List<Item> content = fetchContent(itemIds, orderClause);

        // 5. 전체 카운트 쿼리 생성
        JPAQuery<Long> countQuery = createCountQuery(whereClause);

        // 6. Page 객체로 변환하여 반환
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanBuilder createWhereClause(ItemSearchRequestDto request) {
        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.hasText(request.keyword())) {
            builder.and(item.nameKr.containsIgnoreCase(request.keyword().trim()));
        }
        if (request.jobId() != null) {
            builder.and(
                new BooleanBuilder()
                    .or(itemJob.jobId.eq(request.jobId()))
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

    private List<OrderSpecifier<?>> createOrderClause(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return List.of(item.itemId.asc());
        }

        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        pageable.getSort().forEach(order -> {
            Path<?> path = sortableProperties.get(order.getProperty());
            if (path != null) {
                orderSpecifiers.add(new OrderSpecifier(order.isAscending() ? Order.ASC : Order.DESC, path));
            }
        });

        orderSpecifiers.add(item.itemId.asc());
        return orderSpecifiers;
    }

    private List<Integer> fetchItemIds(BooleanBuilder where, List<OrderSpecifier<?>> order, Pageable pageable) {
        List<Tuple> tuples = queryFactory
                .select(item.itemId, item.nameKr, equipmentItem.requiredStats.level)
                .from(item)
                .leftJoin(equipmentItem).on(item.itemId.eq(equipmentItem.itemId))
                .leftJoin(itemJob).on(item.itemId.eq(itemJob.itemId))
                .where(where)
                .distinct()
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(order.toArray(new OrderSpecifier[0]))
                .fetch();

        return tuples.stream()
                .map(tuple -> tuple.get(item.itemId))
                .toList();
    }

    private List<Item> fetchContent(List<Integer> itemIds, List<OrderSpecifier<?>> order) {
        return queryFactory
                .selectFrom(item)
                .leftJoin(equipmentItem).on(item.itemId.eq(equipmentItem.itemId)).fetchJoin()
                .where(item.itemId.in(itemIds))
                .orderBy(order.toArray(new OrderSpecifier[0]))
                .fetch();
    }

    private JPAQuery<Long> createCountQuery(BooleanBuilder where) {
        return queryFactory
                .select(item.countDistinct())
                .from(item)
                .leftJoin(equipmentItem).on(item.itemId.eq(equipmentItem.itemId))
                .leftJoin(itemJob).on(item.itemId.eq(itemJob.itemId))
                .where(where);
    }
}