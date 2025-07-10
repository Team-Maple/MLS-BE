package com.maple.api.item.application.dto;

import com.maple.api.item.domain.*;
import com.maple.api.job.domain.Job;
import com.maple.api.job.application.dto.JobDto;
import lombok.Builder;

import java.util.List;

@Builder
public record ItemDetailDto(
        Integer itemId,
        String nameKr,
        String nameEn,
        String descriptionText,
        String itemImageUrl,
        Integer npcPrice,
        String itemType,
        CategoryHierarchyDto categoryHierarchy,
        List<JobDto> availableJobs,
        RequiredStatsDto requiredStats,
        ItemEquipmentStatsDto equipmentStats,
        ItemScrollDetailDto scrollDetail
) {
    
    public static ItemDetailDto toDto(Item item, Category rootCategory, Category leafCategory, List<Job> availableJobs) {
        String itemType = getItemType(item);
        
        return ItemDetailDto.builder()
                .itemId(item.getItemId())
                .nameKr(item.getNameKr())
                .nameEn(item.getNameEn())
                .descriptionText(item.getDescriptionText())
                .itemImageUrl(item.getItemImageUrl())
                .npcPrice(item.getNpcPrice())
                .itemType(itemType)
                .categoryHierarchy(CategoryHierarchyDto.toDto(rootCategory, leafCategory))
                .availableJobs(availableJobs.stream().map(JobDto::toDto).toList())
                .requiredStats(getRequiredStats(item))
                .equipmentStats(getEquipmentStats(item))
                .scrollDetail(getScrollDetail(item))
                .build();
    }
    
    private static String getItemType(Item item) {
        return switch (item) {
            case EquipmentItem e -> "EQUIPMENT";
            case ScrollItem s -> "SCROLL";
            case OtherItem o -> "OTHER";
            case null, default -> "UNKNOWN";
        };
    }
    
    private static RequiredStatsDto getRequiredStats(Item item) {
        if (item instanceof EquipmentItem equipmentItem) {
            return RequiredStatsDto.toDto(equipmentItem.getRequiredStats());
        }
        return null;
    }
    
    private static ItemEquipmentStatsDto getEquipmentStats(Item item) {
        if (item instanceof EquipmentItem equipmentItem) {
            return ItemEquipmentStatsDto.toDto(equipmentItem.getEquipmentStats());
        }
        return null;
    }
    
    private static ItemScrollDetailDto getScrollDetail(Item item) {
        if (item instanceof ScrollItem scrollItem) {
            return ItemScrollDetailDto.toDto(scrollItem.getScrollDetail());
        }
        return null;
    }
}