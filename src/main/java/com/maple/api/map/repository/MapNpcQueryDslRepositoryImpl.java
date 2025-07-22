package com.maple.api.map.repository;

import com.maple.api.map.application.dto.MapNpcDto;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.maple.api.map.domain.QMapNpc.mapNpc;
import static com.maple.api.npc.domain.QNpc.npc;

@Repository
@RequiredArgsConstructor
public class MapNpcQueryDslRepositoryImpl implements MapNpcQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MapNpcDto> findNpcsByMapId(Integer mapId) {
        List<Tuple> tuples = queryFactory
                .select(npc.npcId,
                        npc.nameKr,
                        npc.nameEn,
                        npc.iconUrlDetail)
                .from(mapNpc)
                .join(npc).on(mapNpc.npcId.eq(npc.npcId))
                .where(mapNpc.mapId.eq(mapId))
                .orderBy(npc.npcId.asc()) // 일관된 결과를 위한 기본 정렬
                .fetch();
        
        return tuples.stream()
                .map(tuple -> new MapNpcDto(
                        tuple.get(npc.npcId),
                        tuple.get(npc.nameKr),
                        tuple.get(npc.nameEn),
                        tuple.get(npc.iconUrlDetail)
                ))
                .toList();
    }
}