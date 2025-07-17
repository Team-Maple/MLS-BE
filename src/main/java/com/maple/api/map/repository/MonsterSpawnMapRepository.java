package com.maple.api.map.repository;

import com.maple.api.map.domain.MonsterSpawnMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonsterSpawnMapRepository extends JpaRepository<MonsterSpawnMap, Integer> {
    List<MonsterSpawnMap> findByMonsterId(Integer monsterId);
}