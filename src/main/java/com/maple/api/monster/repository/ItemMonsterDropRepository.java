package com.maple.api.monster.repository;

import com.maple.api.monster.domain.ItemMonsterDrop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemMonsterDropRepository extends JpaRepository<ItemMonsterDrop, Integer> {
    List<ItemMonsterDrop> findByMonsterId(Integer monsterId);
    
    @Query("SELECT imd.itemId FROM ItemMonsterDrop imd WHERE imd.monsterId = :monsterId")
    List<Integer> findItemIdsByMonsterId(Integer monsterId);
}