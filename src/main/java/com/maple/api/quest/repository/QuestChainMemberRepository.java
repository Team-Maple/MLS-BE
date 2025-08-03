package com.maple.api.quest.repository;

import com.maple.api.quest.domain.QuestChainMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestChainMemberRepository extends JpaRepository<QuestChainMember, Integer> {
    Optional<QuestChainMember> findByQuestId(Integer questId);
    List<QuestChainMember> findByChainIdOrderBySequenceOrder(Integer chainId);
}