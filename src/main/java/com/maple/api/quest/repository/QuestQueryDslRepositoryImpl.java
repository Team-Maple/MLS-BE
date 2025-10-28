package com.maple.api.quest.repository;

import com.maple.api.quest.application.dto.QuestSearchRequestDto;
import com.maple.api.quest.domain.Quest;
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

import static com.maple.api.quest.domain.QQuest.quest;

@Repository
@RequiredArgsConstructor
public class QuestQueryDslRepositoryImpl implements QuestQueryDslRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Quest> searchQuests(QuestSearchRequestDto request, Pageable pageable) {
        // 1. WHERE 절 생성
        BooleanBuilder whereClause = createWhereClause(request);

        // 2. ORDER BY 절 생성 (가나다순 고정)
        OrderSpecifier<?> orderClause = quest.nameKr.asc();

        // 3. 최적화된 ID 목록 조회
        List<Integer> questIds = fetchQuestIds(whereClause, orderClause, pageable);
        if (questIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // 4. ID 목록으로 실제 컨텐츠 조회
        List<Quest> content = fetchContent(questIds, orderClause);

        // 5. 전체 카운트 쿼리 생성
        JPAQuery<Long> countQuery = createCountQuery(whereClause);

        // 6. Page 객체로 변환하여 반환
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanBuilder createWhereClause(QuestSearchRequestDto request) {
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(request.keyword())) {
            builder.and(quest.nameKr.containsIgnoreCase(request.keyword().trim()));
        }
        return builder;
    }

    private List<Integer> fetchQuestIds(BooleanBuilder where, OrderSpecifier<?> order, Pageable pageable) {
        return queryFactory
                .select(quest.questId)
                .from(quest)
                .where(where)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(order)
                .fetch();
    }

    private List<Quest> fetchContent(List<Integer> questIds, OrderSpecifier<?> order) {
        return queryFactory
                .selectFrom(quest)
                .where(quest.questId.in(questIds))
                .orderBy(order)
                .fetch();
    }

    private JPAQuery<Long> createCountQuery(BooleanBuilder where) {
        return queryFactory
                .select(quest.count())
                .from(quest)
                .where(where);
    }

    @Override
    public long countQuestsByKeyword(String keyword) {
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(keyword)) {
            builder.and(quest.nameKr.containsIgnoreCase(keyword.trim()));
        }

        Long result = queryFactory
                .select(quest.count())
                .from(quest)
                .where(builder)
                .fetchOne();

        return result != null ? result : 0L;
    }
}
