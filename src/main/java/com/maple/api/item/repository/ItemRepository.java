package com.maple.api.item.repository;

import com.maple.api.item.domain.EquipmentItem;
import com.maple.api.item.domain.Item;
import com.maple.api.item.domain.ScrollItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Integer> {
    @Query("SELECT i FROM EquipmentItem i JOIN FETCH i.equipmentStats WHERE i.itemId = :id")
    Optional<EquipmentItem> findEquipmentDetailById(Integer id);

    @Query("SELECT i FROM ScrollItem i JOIN FETCH i.scrollDetail WHERE i.itemId = :id")
    Optional<ScrollItem> findScrollDetailById(Integer id);
}
