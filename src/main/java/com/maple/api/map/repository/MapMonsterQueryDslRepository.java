package com.maple.api.map.repository;

import com.maple.api.map.application.dto.MapMonsterDto;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface MapMonsterQueryDslRepository {
    List<MapMonsterDto> findMapMonsterDtosByMapId(Integer mapId, Sort sort);
}