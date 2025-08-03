package com.maple.api.quest.repository;

import com.maple.api.quest.domain.QuestReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestRewardRepository extends JpaRepository<QuestReward, Integer> {
    Optional<QuestReward> findByQuestId(Integer questId);
}