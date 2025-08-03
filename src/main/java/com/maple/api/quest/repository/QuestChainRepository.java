package com.maple.api.quest.repository;

import com.maple.api.quest.domain.QuestChain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestChainRepository extends JpaRepository<QuestChain, Integer> {
}