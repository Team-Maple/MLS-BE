package com.maple.api.quest.repository;

import com.maple.api.quest.domain.QuestRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestRequirementRepository extends JpaRepository<QuestRequirement, Integer> {
    List<QuestRequirement> findByQuestId(Integer questId);
}