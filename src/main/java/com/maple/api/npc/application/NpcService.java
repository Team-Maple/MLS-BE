package com.maple.api.npc.application;

import com.maple.api.common.presentation.exception.ApiException;
import com.maple.api.npc.application.dto.NpcDetailDto;
import com.maple.api.npc.application.dto.NpcQuestDto;
import com.maple.api.npc.application.dto.NpcSearchRequestDto;
import com.maple.api.npc.application.dto.NpcSpawnMapDto;
import com.maple.api.npc.application.dto.NpcSummaryDto;
import com.maple.api.npc.domain.Npc;
import com.maple.api.npc.exception.NpcException;
import com.maple.api.npc.repository.NpcQueryDslRepository;
import com.maple.api.npc.repository.NpcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NpcService {

    private final NpcQueryDslRepository npcQueryDslRepository;
    private final NpcRepository npcRepository;

    @Transactional(readOnly = true)
    public Page<NpcSummaryDto> searchNpcs(NpcSearchRequestDto request, Pageable pageable) {
        Page<Npc> npcPage = npcQueryDslRepository.searchNpcs(request, pageable);
        return npcPage.map(NpcSummaryDto::toDto);
    }

    @Transactional(readOnly = true)
    public NpcDetailDto getNpcDetail(Integer npcId) {
        Npc npc = npcRepository.findByNpcId(npcId)
                .orElseThrow(() -> ApiException.of(NpcException.NPC_NOT_FOUND));
        
        return NpcDetailDto.toDto(npc);
    }

    @Transactional(readOnly = true)
    public List<NpcSpawnMapDto> getNpcSpawnMaps(Integer npcId) {
        if (!npcRepository.existsByNpcId(npcId)) {
            throw ApiException.of(NpcException.NPC_NOT_FOUND);
        }

        return npcQueryDslRepository.findNpcSpawnMapsByNpcId(npcId);
    }

    @Transactional(readOnly = true)
    public List<NpcQuestDto> getNpcQuests(Integer npcId, Sort sort) {
        if (!npcRepository.existsByNpcId(npcId)) {
            throw ApiException.of(NpcException.NPC_NOT_FOUND);
        }

        return npcQueryDslRepository.findNpcQuestsByNpcId(npcId, sort);
    }
}