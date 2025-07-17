package com.maple.api.map.repository;

import com.maple.api.map.domain.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MapRepository extends JpaRepository<Map, Integer> {
    List<Map> findByMapIdIn(List<Integer> mapIds);
}