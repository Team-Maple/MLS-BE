package com.maple.api.search.repository;

import com.maple.api.search.domain.QVwSearchSummary;
import com.maple.api.search.domain.VwSearchSummary;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
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

        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(vwSearchSummary, pageable);

        List<VwSearchSummary> content = queryFactory
                .selectFrom(vwSearchSummary)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier<?>[0]))
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(vwSearchSummary.count())
                .from(vwSearchSummary)
                .where(builder);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private List<OrderSpecifier<?>> buildOrderSpecifiers(QVwSearchSummary vw, Pageable pageable) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        pageable.getSort().forEach(order -> {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            String property = order.getProperty();
            ComparableExpressionBase<?> expression = switch (property) {
                case "name" -> vw.name;
                case "level" -> vw.level;
                case "type" -> vw.type;
                case "originalId" -> vw.originalId;
                default -> null;
            };
            if (expression != null) {
                orders.add(new OrderSpecifier<>(direction, expression));
            }
        });

        if (orders.isEmpty()) {
            orders.add(vw.name.asc());
        }

        return orders;
    }

    @Override
    public long countByKeyword(String keyword) {
        QVwSearchSummary vwSearchSummary = QVwSearchSummary.vwSearchSummary;

        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(keyword)) {
            builder.and(vwSearchSummary.name.contains(keyword));
        }

        Long result = queryFactory
                .select(vwSearchSummary.count())
                .from(vwSearchSummary)
                .where(builder)
                .fetchOne();

        return result != null ? result : 0L;
    }

}
