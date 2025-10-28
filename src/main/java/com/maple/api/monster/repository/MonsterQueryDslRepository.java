package com.maple.api.monster.repository;

import com.maple.api.monster.application.dto.MonsterDropItemDto;
import com.maple.api.monster.application.dto.MonsterSearchRequestDto;
import com.maple.api.monster.application.dto.MonsterSpawnMapDto;
import com.maple.api.monster.domain.Monster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface MonsterQueryDslRepository {
    Page<Monster> searchMonsters(MonsterSearchRequestDto request, Pageable pageable);
    List<MonsterSpawnMapDto> findMonsterSpawnMapsByMonsterId(Integer monsterId, Sort sort);
    List<MonsterDropItemDto> findMonsterDropItemsByMonsterId(Integer monsterId, Sort sort);
    long countMonstersByKeyword(String keyword);
}
