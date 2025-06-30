package com.maple.api.search.application.dto;

import com.maple.api.search.domain.VwSearchSummary;

public record SearchSummaryDto(Integer originalId, String name, String imageUrl, String type) {
    public static SearchSummaryDto toDto(VwSearchSummary entity) {
        return new SearchSummaryDto(entity.getOriginalId(), entity.getName(), entity.getImageUrl(), entity.getType());
    }
}
