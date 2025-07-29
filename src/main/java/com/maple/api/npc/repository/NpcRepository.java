package com.maple.api.npc.repository;

import com.maple.api.npc.domain.Npc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NpcRepository extends JpaRepository<Npc, Integer> {
    Optional<Npc> findByNpcId(Integer npcId);
    boolean existsByNpcId(Integer npcId);
}