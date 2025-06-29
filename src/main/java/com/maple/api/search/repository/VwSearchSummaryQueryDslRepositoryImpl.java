package com.maple.api.search.repository;

import com.maple.api.search.domain.QVwSearchSummary;
import com.maple.api.search.domain.VwSearchSummary;
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

@Repository
@RequiredArgsConstructor
public class VwSearchSummaryQueryDslRepositoryImpl implements VwSearchSummaryQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<VwSearchSummary> search(String keyword, Pageable pageable) {
        QVwSearchSummary vwSearchSummary = QVwSearchSummary.vwSearchSummary;

        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.hasText(keyword)) {
            builder.and(vwSearchSummary.name.contains(keyword));
        }

        List<VwSearchSummary> content = queryFactory
                .selectFrom(vwSearchSummary)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(vwSearchSummary.name.asc())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(vwSearchSummary.count())
                .from(vwSearchSummary)
                .where(builder);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

}