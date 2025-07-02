package com.maple.api.map.repository;

import com.maple.api.map.application.dto.MapSearchRequestDto;
import com.maple.api.map.domain.Map;
import com.querydsl.core.BooleanBuilder;
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
    public Page<Map> searchMaps(MapSearchRequestDto request, Pageable pageable) {
        BooleanBuilder whereClause = createWhereClause(request);

        List<Map> content = queryFactory
                .selectFrom(map)
                .where(whereClause)
                .orderBy(map.nameKr.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(map.count())
                .from(map)
                .where(whereClause);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanBuilder createWhereClause(MapSearchRequestDto request) {
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(request.keyword())) {
            builder.and(map.nameKr.containsIgnoreCase(request.keyword()));
        }
        return builder;
    }
}