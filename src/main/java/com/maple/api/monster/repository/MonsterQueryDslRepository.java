package com.maple.api.monster.repository;

import com.maple.api.monster.application.dto.MonsterSearchRequestDto;
import com.maple.api.monster.domain.Monster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MonsterQueryDslRepository {
    Page<Monster> searchMonsters(MonsterSearchRequestDto request, Pageable pageable);
}
