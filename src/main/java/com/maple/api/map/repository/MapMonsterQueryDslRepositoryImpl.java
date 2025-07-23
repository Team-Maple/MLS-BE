package com.maple.api.map.repository;

import com.maple.api.map.application.dto.MapMonsterDto;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.maple.api.map.domain.QMonsterSpawnMap.monsterSpawnMap;
import static com.maple.api.monster.domain.QMonster.monster;

@Repository
@RequiredArgsConstructor
public class MapMonsterQueryDslRepositoryImpl implements MapMonsterQueryDslRepository {

    private final JPAQueryFactory queryFactory;
    
    private final Map<String, Path<?>> sortableProperties = Map.of(
            "level", monster.level,
            "maxSpawnCount", monsterSpawnMap.maxSpawnCount,
            "monsterId", monster.monsterId
    );

    @Override
    public List<MapMonsterDto> findMapMonsterDtosByMapId(Integer mapId, Sort sort) {
        List<OrderSpecifier<?>> orderClause = createOrderClause(sort);
        
        List<Tuple> tuples = queryFactory
                .select(monster.monsterId,
                        monster.nameKr,
                        monster.level,
                        monsterSpawnMap.maxSpawnCount,
                        monster.imageUrl)
                .from(monsterSpawnMap)
                .join(monster).on(monsterSpawnMap.monsterId.eq(monster.monsterId))
                .where(monsterSpawnMap.mapId.eq(mapId))
                .orderBy(orderClause.toArray(new OrderSpecifier[0]))
                .fetch();
        
        return tuples.stream()
                .map(tuple -> new MapMonsterDto(
                        tuple.get(monster.monsterId),
                        tuple.get(monster.nameKr),
                        tuple.get(monster.level),
                        tuple.get(monsterSpawnMap.maxSpawnCount),
                        tuple.get(monster.imageUrl)
                ))
                .toList();
    }

    private List<OrderSpecifier<?>> createOrderClause(Sort sort) {
        if (sort.isUnsorted()) {
            return List.of(monster.monsterId.asc());
        }

        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        sort.forEach(order -> {
            Path<?> path = sortableProperties.get(order.getProperty());
            if (path != null) {
                orderSpecifiers.add(new OrderSpecifier(order.isAscending() ? Order.ASC : Order.DESC, path));
            }
        });

        // 기본 정렬 추가 (일관된 결과를 위해)
        orderSpecifiers.add(monster.monsterId.asc());
        return orderSpecifiers;
    }
}