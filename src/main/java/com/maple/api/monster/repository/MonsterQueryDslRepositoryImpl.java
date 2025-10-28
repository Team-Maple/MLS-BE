package com.maple.api.monster.repository;

import com.maple.api.monster.application.dto.MonsterDropItemDto;
import com.maple.api.monster.application.dto.MonsterSearchRequestDto;
import com.maple.api.monster.application.dto.MonsterSpawnMapDto;
import com.maple.api.monster.domain.Monster;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.maple.api.item.domain.QEquipmentItem.equipmentItem;
import static com.maple.api.item.domain.QItem.item;
import static com.maple.api.map.domain.QMap.map;
import static com.maple.api.map.domain.QMonsterSpawnMap.monsterSpawnMap;
import static com.maple.api.monster.domain.QItemMonsterDrop.itemMonsterDrop;
import static com.maple.api.monster.domain.QMonster.monster;

@Repository
@RequiredArgsConstructor
public class MonsterQueryDslRepositoryImpl implements MonsterQueryDslRepository {
    private final JPAQueryFactory queryFactory;
    
    private final Map<String, Path<?>> sortableProperties = Map.of(
            "name", monster.nameKr,
            "level", monster.level,
            "exp", monster.exp
    );
    
    private final Map<String, Path<?>> spawnMapSortableProperties = Map.of(
            "maxSpawnCount", monsterSpawnMap.maxSpawnCount,
            "mapId", map.mapId
    );
    
    private final Map<String, Path<?>> dropItemSortableProperties = Map.of(
            "dropRate", itemMonsterDrop.dropRate,
            "itemId", item.itemId,
            "level", equipmentItem.requiredStats.level
    );

    @Override
    public Page<Monster> searchMonsters(MonsterSearchRequestDto request, Pageable pageable) {
        // 1. WHERE 절 생성
        BooleanBuilder whereClause = createWhereClause(request);

        // 2. ORDER BY 절 생성
        List<OrderSpecifier<?>> orderClause = createOrderClause(pageable);

        // 3. 최적화된 ID 목록 조회
        List<Integer> monsterIds = fetchMonsterIds(whereClause, orderClause, pageable);
        if (monsterIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // 4. ID 목록으로 실제 컨텐츠 조회
        List<Monster> content = fetchContent(monsterIds, orderClause);

        // 5. 전체 카운트 쿼리 생성
        JPAQuery<Long> countQuery = createCountQuery(whereClause);

        // 6. Page 객체로 변환하여 반환
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanBuilder createWhereClause(MonsterSearchRequestDto request) {
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(request.keyword())) {
            builder.and(monster.nameKr.containsIgnoreCase(request.keyword().trim()));
        }
        if (request.minLevel() != null) {
            builder.and(monster.level.goe(request.minLevel()));
        }
        if (request.maxLevel() != null) {
            builder.and(monster.level.loe(request.maxLevel()));
        }
        return builder;
    }

    private List<OrderSpecifier<?>> createOrderClause(Pageable pageable) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
        boolean nameSortSpecified = false;

        for (Sort.Order order : pageable.getSort()) {
            Path<?> path = sortableProperties.get(order.getProperty());
            if (path != null) {
                orderSpecifiers.add(new OrderSpecifier(order.isAscending() ? Order.ASC : Order.DESC, path));
                if ("name".equals(order.getProperty())) {
                    nameSortSpecified = true;
                }
            }
        }

        if (orderSpecifiers.isEmpty()) {
            orderSpecifiers.add(monster.nameKr.asc());
        } else if (!nameSortSpecified) {
            orderSpecifiers.add(monster.nameKr.asc());
        }

        return orderSpecifiers;
    }

    private List<Integer> fetchMonsterIds(BooleanBuilder where, List<OrderSpecifier<?>> order, Pageable pageable) {
        return queryFactory
                .select(monster.monsterId)
                .from(monster)
                .where(where)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(order.toArray(new OrderSpecifier[0]))
                .fetch();
    }

    private List<Monster> fetchContent(List<Integer> monsterIds, List<OrderSpecifier<?>> order) {
        return queryFactory
                .selectFrom(monster)
                .where(monster.monsterId.in(monsterIds))
                .orderBy(order.toArray(new OrderSpecifier[0]))
                .fetch();
    }

    private JPAQuery<Long> createCountQuery(BooleanBuilder where) {
        return queryFactory
                .select(monster.count())
                .from(monster)
                .where(where);
    }

    @Override
    public List<MonsterSpawnMapDto> findMonsterSpawnMapsByMonsterId(Integer monsterId, Sort sort) {
        List<OrderSpecifier<?>> orderSpecifiers = createSpawnMapOrderClause(sort);
        
        return queryFactory
                .select(Projections.constructor(MonsterSpawnMapDto.class,
                        map.mapId,
                        map.nameKr,
                        map.regionName,
                        map.detailName,
                        map.topRegionName,
                        map.iconUrl,
                        monsterSpawnMap.maxSpawnCount))
                .from(monsterSpawnMap)
                .join(map).on(monsterSpawnMap.mapId.eq(map.mapId))
                .where(monsterSpawnMap.monsterId.eq(monsterId))
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .fetch();
    }
    
    @Override
    public List<MonsterDropItemDto> findMonsterDropItemsByMonsterId(Integer monsterId, Sort sort) {
        List<OrderSpecifier<?>> orderSpecifiers = createDropItemOrderClause(sort);
        
        return queryFactory
                .select(Projections.constructor(MonsterDropItemDto.class,
                        item.itemId,
                        item.nameKr,
                        itemMonsterDrop.dropRate,
                        item.itemImageUrl,
                        equipmentItem.requiredStats.level.coalesce(0)))
                .from(itemMonsterDrop)
                .join(item).on(itemMonsterDrop.itemId.eq(item.itemId))
                .leftJoin(equipmentItem).on(item.itemId.eq(equipmentItem.itemId))
                .where(itemMonsterDrop.monsterId.eq(monsterId))
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .fetch();
    }
    
    private List<OrderSpecifier<?>> createSpawnMapOrderClause(Sort sort) {
        if (sort.isUnsorted()) {
            return List.of(monsterSpawnMap.maxSpawnCount.desc());
        }

        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        sort.forEach(order -> {
            Path<?> path = spawnMapSortableProperties.get(order.getProperty());
            if (path != null) {
                orderSpecifiers.add(new OrderSpecifier(order.isAscending() ? Order.ASC : Order.DESC, path));
            }
        });

        if (orderSpecifiers.isEmpty()) {
            orderSpecifiers.add(monsterSpawnMap.maxSpawnCount.desc());
        }
        
        return orderSpecifiers;
    }
    
    private List<OrderSpecifier<?>> createDropItemOrderClause(Sort sort) {
        if (sort.isUnsorted()) {
            return List.of(itemMonsterDrop.dropRate.desc());
        }

        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        sort.forEach(order -> {
            Path<?> path = dropItemSortableProperties.get(order.getProperty());
            if (path != null) {
                orderSpecifiers.add(new OrderSpecifier(order.isAscending() ? Order.ASC : Order.DESC, path));
            }
        });

        if (orderSpecifiers.isEmpty()) {
            orderSpecifiers.add(itemMonsterDrop.dropRate.desc());
        }
        
        return orderSpecifiers;
    }

    @Override
    public long countMonstersByKeyword(String keyword) {
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(keyword)) {
            builder.and(monster.nameKr.containsIgnoreCase(keyword.trim()));
        }

        Long result = queryFactory
                .select(monster.count())
                .from(monster)
                .where(builder)
                .fetchOne();

        return result != null ? result : 0L;
    }

}
