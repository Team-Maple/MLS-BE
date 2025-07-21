package com.maple.api.monster.repository;

import com.maple.api.monster.domain.MonsterTypeEffectiveness;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonsterTypeEffectivenessRepository extends JpaRepository<MonsterTypeEffectiveness, Integer> {
    Optional<MonsterTypeEffectiveness> findByMonsterId(Integer monsterId);
}