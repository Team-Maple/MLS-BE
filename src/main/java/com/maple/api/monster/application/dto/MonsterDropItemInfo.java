package com.maple.api.monster.application.dto;

import com.maple.api.item.domain.EquipmentItem;
import com.maple.api.item.domain.Item;
import com.maple.api.monster.domain.ItemMonsterDrop;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "몬스터 드롭 아이템 정보")
public record MonsterDropItemInfo(
        @Schema(description = "아이템 ID", example = "2000000")
        Integer itemId,
        
        @Schema(description = "아이템 이름", example = "빨간 포션")
        String itemName,

        @Schema(description = "아이템 착용 가능 레벨", example = "0")
        Integer requiredLevel,
        
        @Schema(description = "아이템 이미지 URL", example = "https://maplestory.io/api/gms/62/item/2000000/icon?resize=2")
        String itemImageUrl,

        @Schema(description = "드롭 확률", example = "0.1")
        Double dropRate
) {
    public static MonsterDropItemInfo toDto(ItemMonsterDrop dropItem, Item item) {
        if (item == null) {
            return null;
        }

        return new MonsterDropItemInfo(
                dropItem.getItemId(),
                item.getNameKr(),
                item instanceof EquipmentItem e ? e.getRequiredStats().getLevel() : 0,
                item.getItemImageUrl(),
                dropItem.getDropRate()
        );
    }
}