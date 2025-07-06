package com.maple.api.monster.application;

import com.maple.api.monster.application.dto.MonsterSearchRequestDto;
import com.maple.api.monster.application.dto.MonsterSummaryDto;
import com.maple.api.monster.repository.MonsterQueryDslRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MonsterService {

    private final MonsterQueryDslRepository monsterQueryDslRepository;

    @Transactional(readOnly = true)
    public Page<MonsterSummaryDto> searchMonsters(MonsterSearchRequestDto request, Pageable pageable) {
        return monsterQueryDslRepository.searchMonsters(request, pageable).map(MonsterSummaryDto::toDto);
    }
}