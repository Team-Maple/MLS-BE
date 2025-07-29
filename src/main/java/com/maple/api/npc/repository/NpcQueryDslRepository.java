package com.maple.api.npc.repository;

import com.maple.api.npc.application.dto.NpcQuestDto;
import com.maple.api.npc.application.dto.NpcSearchRequestDto;
import com.maple.api.npc.application.dto.NpcSpawnMapDto;
import com.maple.api.npc.domain.Npc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface NpcQueryDslRepository {
    Page<Npc> searchNpcs(NpcSearchRequestDto request, Pageable pageable);
    List<NpcSpawnMapDto> findNpcSpawnMapsByNpcId(Integer npcId);
    List<NpcQuestDto> findNpcQuestsByNpcId(Integer npcId, Sort sort);
}