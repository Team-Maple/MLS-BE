package com.maple.api.item.application.dto;

import com.maple.api.item.domain.Item;

public record ItemSummaryDto(Integer itemId, String name, String imageUrl, String type) {
    public static ItemSummaryDto toDto(Item entity) {
        return new ItemSummaryDto(
                entity.getItemId(), 
                entity.getNameKr(), 
                entity.getItemImageUrl(),
                "item"
        );
    }
}