package com.maple.api.item.application.dto;

import com.maple.api.item.domain.*;
import com.maple.api.job.domain.Job;
import com.maple.api.job.application.dto.JobDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "아이템 상세 정보 DTO")
public record ItemDetailDto(
        @Schema(description = "아이템 ID", example = "1002001")
        Integer itemId,
        
        @Schema(description = "아이템명 (한국어)", example = "메탈 기어")
        String nameKr,
        
        @Schema(description = "아이템명 (영어)", example = "Metal Gear")
        String nameEn,
        
        @Schema(description = "아이템 설명", example = "메탈 기어입니다.")
        String descriptionText,
        
        @Schema(description = "아이템 이미지 URL", example = "https://maplestory.io/api/gms/62/item/1002001/icon?resize=2")
        String itemImageUrl,
        
        @Schema(description = "NPC 판매 가격", example = "1500")
        Integer npcPrice,
        
        @Schema(description = "아이템 타입", example = "EQUIPMENT", allowableValues = {"EQUIPMENT", "SCROLL", "OTHER", "UNKNOWN"})
        String itemType,
        
        @Schema(description = "카테고리 계층 정보")
        CategoryHierarchyDto categoryHierarchy,
        
        @Schema(description = "착용 가능한 직업 목록")
        List<JobDto> availableJobs,
        
        @Schema(description = "필요 스탯 정보 (장비 아이템의 경우)")
        RequiredStatsDto requiredStats,
        
        @Schema(description = "장비 스탯 정보 (장비 아이템의 경우)")
        ItemEquipmentStatsDto equipmentStats,
        
        @Schema(description = "스크롤 상세 정보 (스크롤 아이템의 경우)")
        ItemScrollDetailDto scrollDetail,
        
        @Schema(description = "로그인 사용자의 북마크 여부", example = "false")
        boolean isBookmarked
) {
    
    public static ItemDetailDto toDto(Item item, Category rootCategory, Category leafCategory, List<Job> availableJobs) {
        return toDto(item, rootCategory, leafCategory, availableJobs, false);
    }

    public static ItemDetailDto toDto(Item item, Category rootCategory, Category leafCategory, List<Job> availableJobs, boolean isBookmarked) {
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
                .isBookmarked(isBookmarked)
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
