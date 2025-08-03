package com.maple.api.quest.repository;

import com.maple.api.quest.domain.QuestAllowedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestAllowedJobRepository extends JpaRepository<QuestAllowedJob, Integer> {
    List<QuestAllowedJob> findByQuestId(Integer questId);
}