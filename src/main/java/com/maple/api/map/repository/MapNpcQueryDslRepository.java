package com.maple.api.map.repository;

import com.maple.api.map.application.dto.MapNpcDto;

import java.util.List;

public interface MapNpcQueryDslRepository {
    List<MapNpcDto> findNpcsByMapId(Integer mapId);
}