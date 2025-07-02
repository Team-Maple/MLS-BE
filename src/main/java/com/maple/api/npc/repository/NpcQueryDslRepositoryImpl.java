package com.maple.api.npc.repository;

import com.maple.api.npc.application.dto.NpcSearchRequestDto;
import com.maple.api.npc.domain.Npc;
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

import static com.maple.api.npc.domain.QNpc.npc;

@Repository
@RequiredArgsConstructor
public class NpcQueryDslRepositoryImpl implements NpcQueryDslRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Npc> searchNpcs(NpcSearchRequestDto request, Pageable pageable) {
        // 1. WHERE 절 생성
        BooleanBuilder whereClause = createWhereClause(request);

        // 2. ORDER BY 절 생성 (가나다순 고정)
        OrderSpecifier<?> orderClause = npc.nameKr.asc();

        // 3. 최적화된 ID 목록 조회
        List<Integer> npcIds = fetchNpcIds(whereClause, orderClause, pageable);
        if (npcIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // 4. ID 목록으로 실제 컨텐츠 조회
        List<Npc> content = fetchContent(npcIds, orderClause);

        // 5. 전체 카운트 쿼리 생성
        JPAQuery<Long> countQuery = createCountQuery(whereClause);

        // 6. Page 객체로 변환하여 반환
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanBuilder createWhereClause(NpcSearchRequestDto request) {
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(request.keyword())) {
            builder.and(npc.nameKr.containsIgnoreCase(request.keyword().trim()));
        }
        return builder;
    }

    private List<Integer> fetchNpcIds(BooleanBuilder where, OrderSpecifier<?> order, Pageable pageable) {
        return queryFactory
                .select(npc.npcId)
                .from(npc)
                .where(where)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(order)
                .fetch();
    }

    private List<Npc> fetchContent(List<Integer> npcIds, OrderSpecifier<?> order) {
        return queryFactory
                .selectFrom(npc)
                .where(npc.npcId.in(npcIds))
                .orderBy(order)
                .fetch();
    }

    private JPAQuery<Long> createCountQuery(BooleanBuilder where) {
        return queryFactory
                .select(npc.count())
                .from(npc)
                .where(where);
    }
}