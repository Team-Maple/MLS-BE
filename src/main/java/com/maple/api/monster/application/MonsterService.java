package com.maple.api.monster.application;

import com.maple.api.common.presentation.exception.ApiException;
import com.maple.api.monster.application.dto.*;
import com.maple.api.monster.domain.Monster;
import com.maple.api.monster.exception.MonsterException;
import com.maple.api.monster.repository.MonsterQueryDslRepository;
import com.maple.api.monster.repository.MonsterRepository;
import com.maple.api.monster.repository.MonsterTypeEffectivenessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MonsterService {

    private final MonsterQueryDslRepository monsterQueryDslRepository;
    private final MonsterRepository monsterRepository;
    private final MonsterTypeEffectivenessRepository monsterTypeEffectivenessRepository;

    @Transactional(readOnly = true)
    public Page<MonsterSummaryDto> searchMonsters(MonsterSearchRequestDto request, Pageable pageable) {
        return monsterQueryDslRepository.searchMonsters(request, pageable).map(MonsterSummaryDto::toDto);
    }

    @Transactional(readOnly = true)
    public MonsterDetailDto getMonsterDetail(Integer monsterId) {
        // 1. Monster 조회
        Monster monster = monsterRepository.findByMonsterId(monsterId)
                .orElseThrow(() -> ApiException.of(MonsterException.MONSTER_NOT_FOUND));

        // 2. 속성 효과
        MonsterTypeEffectivenessDto typeEffectivenessDto = monsterTypeEffectivenessRepository.findByMonsterId(monsterId)
                .map(MonsterTypeEffectivenessDto::toDto)
                .orElse(null);

        return MonsterDetailDto.toDto(monster, typeEffectivenessDto);
    }

    @Transactional(readOnly = true)
    public List<MonsterSpawnMapDto> getMonsterSpawnMaps(Integer monsterId, Sort sort) {
        if (!monsterRepository.existsByMonsterId(monsterId)) {
            throw ApiException.of(MonsterException.MONSTER_NOT_FOUND);
        }

        return monsterQueryDslRepository.findMonsterSpawnMapsByMonsterId(monsterId, sort);
    }

    @Transactional(readOnly = true)
    public List<MonsterDropItemDto> getMonsterDropItems(Integer monsterId, Sort sort) {
        if (!monsterRepository.existsByMonsterId(monsterId)) {
            throw ApiException.of(MonsterException.MONSTER_NOT_FOUND);
        }

        return monsterQueryDslRepository.findMonsterDropItemsByMonsterId(monsterId, sort);
    }


}