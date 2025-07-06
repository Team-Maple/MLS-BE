package com.maple.api.monster.repository;

import com.maple.api.monster.application.dto.MonsterSearchRequestDto;
import com.maple.api.monster.domain.Monster;
import com.querydsl.core.BooleanBuilder;
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
        if (pageable.getSort().isUnsorted()) {
            return List.of(monster.monsterId.asc());
        }

        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        pageable.getSort().forEach(order -> {
            Path<?> path = sortableProperties.get(order.getProperty());
            if (path != null) {
                orderSpecifiers.add(new OrderSpecifier(order.isAscending() ? Order.ASC : Order.DESC, path));
            }
        });

        orderSpecifiers.add(monster.monsterId.asc());
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

}
