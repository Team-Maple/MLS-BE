package com.maple.api.monster.repository;

import com.maple.api.monster.domain.Monster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonsterRepository extends JpaRepository<Monster, Integer> {
    Optional<Monster> findByMonsterId(Integer monsterId);
}