package com.maple.api.map.repository;

import com.maple.api.map.application.dto.MapSearchRequestDto;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.maple.api.map.domain.QMap.map;

@Repository
@RequiredArgsConstructor
public class MapQueryDslRepositoryImpl implements MapQueryDslRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<com.maple.api.map.domain.Map> searchMaps(MapSearchRequestDto request, Pageable pageable) {
        // 1. WHERE 절 생성
        BooleanBuilder whereClause = createWhereClause(request);

        // 2. ORDER BY 절 생성 (가나다순 고정)
        OrderSpecifier<?> orderClause = map.nameKr.asc();

        // 3. 최적화된 ID 목록 조회
        List<Integer> mapIds = fetchMapIds(whereClause, orderClause, pageable);
        if (mapIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // 4. ID 목록으로 실제 컨텐츠 조회
        List<com.maple.api.map.domain.Map> content = fetchContent(mapIds, orderClause);

        // 5. 전체 카운트 쿼리 생성
        JPAQuery<Long> countQuery = createCountQuery(whereClause);

        // 6. Page 객체로 변환하여 반환
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanBuilder createWhereClause(MapSearchRequestDto request) {
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(request.keyword())) {
            builder.and(map.nameKr.containsIgnoreCase(request.keyword().trim()));
        }
        return builder;
    }

    private List<Integer> fetchMapIds(BooleanBuilder where, OrderSpecifier<?> order, Pageable pageable) {
        return queryFactory
                .select(map.mapId)
                .from(map)
                .where(where)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(order)
                .fetch();
    }

    private List<com.maple.api.map.domain.Map> fetchContent(List<Integer> mapIds, OrderSpecifier<?> order) {
        return queryFactory
                .selectFrom(map)
                .where(map.mapId.in(mapIds))
                .orderBy(order)
                .fetch();
    }

    private JPAQuery<Long> createCountQuery(BooleanBuilder where) {
        return queryFactory
                .select(map.count())
                .from(map)
                .where(where);
    }
}