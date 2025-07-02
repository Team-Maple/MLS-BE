package com.maple.api.map.application.dto;

import com.maple.api.map.domain.Map;

public record MapSummaryDto(Integer mapId, String name, String imageUrl, String type) {
    public static MapSummaryDto toDto(Map entity) {
        return new MapSummaryDto(entity.getMapId(), entity.getNameKr(), entity.getIconUrl(), "map");
    }
}