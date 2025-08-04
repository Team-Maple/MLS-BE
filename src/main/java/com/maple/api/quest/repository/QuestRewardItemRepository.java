package com.maple.api.quest.repository;

import com.maple.api.quest.domain.QuestRewardItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestRewardItemRepository extends JpaRepository<QuestRewardItem, Integer> {
    List<QuestRewardItem> findByQuestId(Integer questId);
}