package com.maple.api.quest.repository;

import com.maple.api.quest.domain.PrerequisiteQuest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrerequisiteQuestRepository extends JpaRepository<PrerequisiteQuest, Integer> {
    List<PrerequisiteQuest> findByQuestIdAndStateIn(Integer questId, List<Integer> states);
    List<PrerequisiteQuest> findByRequiredToStartQuestIdAndStateIn(Integer requiredToStartQuestId, List<Integer> states);
}
