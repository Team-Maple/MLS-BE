package com.maple.api.monster.application;

import com.maple.api.item.domain.Item;
import com.maple.api.item.repository.ItemRepository;
import com.maple.api.map.domain.MonsterSpawnMap;
import com.maple.api.map.repository.MapRepository;
import com.maple.api.map.repository.MonsterSpawnMapRepository;
import com.maple.api.monster.application.dto.*;
import com.maple.api.monster.domain.ItemMonsterDrop;
import com.maple.api.monster.domain.Monster;
import com.maple.api.monster.repository.ItemMonsterDropRepository;
import com.maple.api.monster.repository.MonsterQueryDslRepository;
import com.maple.api.monster.repository.MonsterRepository;
import com.maple.api.monster.repository.MonsterTypeEffectivenessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonsterService {

    private final MonsterQueryDslRepository monsterQueryDslRepository;
    private final MonsterRepository monsterRepository;
    private final MonsterSpawnMapRepository monsterSpawnMapRepository;
    private final ItemMonsterDropRepository itemMonsterDropRepository;
    private final MapRepository mapRepository;
    private final ItemRepository itemRepository;
    private final MonsterTypeEffectivenessRepository monsterTypeEffectivenessRepository;

    @Transactional(readOnly = true)
    public Page<MonsterSummaryDto> searchMonsters(MonsterSearchRequestDto request, Pageable pageable) {
        return monsterQueryDslRepository.searchMonsters(request, pageable).map(MonsterSummaryDto::toDto);
    }

    @Transactional(readOnly = true)
    public MonsterDetailDto getMonsterDetail(Integer monsterId) {
        // 1. Monster 조회
        Monster monster = monsterRepository.findByMonsterId(monsterId)
                // TODO: Domain Exception 병합 후 수정
                .orElseThrow(() -> new IllegalArgumentException("Monster not found with id: " + monsterId));
                //.orElseThrow(() -> throw ApiException.of(MapException.NOT_FOUND));

        // 2. 연관 데이터 조회
        List<MonsterSpawnMap> spawnMaps = monsterSpawnMapRepository.findByMonsterId(monsterId);
        List<ItemMonsterDrop> dropItems = itemMonsterDropRepository.findByMonsterId(monsterId);

        // 3. 연관 엔티티 조회를 위한 ID 추출
        List<Integer> mapIds = extractMapIds(spawnMaps);
        List<Integer> itemIds = extractItemIds(dropItems);

        // 4. 연관 엔티티 일괄 조회 및 Map으로 변환
        Map<Integer, com.maple.api.map.domain.Map> mapById = fetchMapsAsMap(mapIds);
        Map<Integer, Item> itemById = fetchItemsAsMap(itemIds);

        // 5. DTO 변환 및 Response 생성
        List<MonsterSpawnMapDto> spawnMapDtos = convertToSpawnMapInfos(spawnMaps, mapById);
        List<MonsterDropItemDto> dropItemDtos = convertToDropItemInfos(dropItems, itemById);

        // 6. 속성 효과
        MonsterTypeEffectivenessDto typeEffectivenessDto = monsterTypeEffectivenessRepository.findByMonsterId(monsterId)
                .map(MonsterTypeEffectivenessDto::toDto)
                .orElse(null);

        return MonsterDetailDto.toDto(monster, spawnMapDtos, dropItemDtos, typeEffectivenessDto);
    }

    // 추출 메서드들
    private List<Integer> extractMapIds(List<MonsterSpawnMap> spawnMaps) {
        return spawnMaps.stream()
                .map(MonsterSpawnMap::getMapId)
                .distinct()
                .toList();
    }

    private List<Integer> extractItemIds(List<ItemMonsterDrop> dropItems) {
        return dropItems.stream()
                .map(ItemMonsterDrop::getItemId)
                .distinct()
                .toList();
    }

    // 조회 메서드들
    private Map<Integer, com.maple.api.map.domain.Map> fetchMapsAsMap(List<Integer> mapIds) {
        if (mapIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return mapRepository.findByMapIdIn(mapIds).stream()
                .collect(Collectors.toMap(com.maple.api.map.domain.Map::getMapId, Function.identity()));
    }

    private Map<Integer, Item> fetchItemsAsMap(List<Integer> itemIds) {
        if (itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return itemRepository.findByItemIdIn(itemIds).stream()
                .collect(Collectors.toMap(Item::getItemId, Function.identity()));
    }

    // 변환 메서드들
    private List<MonsterSpawnMapDto> convertToSpawnMapInfos(
            List<MonsterSpawnMap> spawnMaps,
            Map<Integer, com.maple.api.map.domain.Map> mapById
    ) {
        return spawnMaps.stream()
                .map(spawnMap -> {
                    com.maple.api.map.domain.Map map = mapById.get(spawnMap.getMapId());
                    // TODO: 도메인 Exception 병합 후 변경
                    // if (map == null) {
                    //     throw ApiException.of(MapException.NOT_FOUND);
                    // }
                    return MonsterSpawnMapDto.toDto(spawnMap, map);
                })
                .toList();
    }

    private List<MonsterDropItemDto> convertToDropItemInfos(
            List<ItemMonsterDrop> dropItems,
            Map<Integer, Item> itemById
    ) {
        return dropItems.stream()
                .map(dropItem -> {
                    Item item = itemById.get(dropItem.getItemId());
                    // TODO: 도메인 Exception 병합 후 변경
                    // if (item == null) {
                    //     throw ApiException.of(MapException.NOT_FOUND);
                    // }
                    return MonsterDropItemDto.toDto(dropItem, item);
                })
                .toList();
    }
}