package com.maple.api.npc.application;

import com.maple.api.npc.application.dto.NpcSearchRequestDto;
import com.maple.api.npc.application.dto.NpcSummaryDto;
import com.maple.api.npc.domain.Npc;
import com.maple.api.npc.repository.NpcQueryDslRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NpcService {

    private final NpcQueryDslRepository npcQueryDslRepository;

    @Transactional(readOnly = true)
    public Page<NpcSummaryDto> searchNpcs(NpcSearchRequestDto request, Pageable pageable) {
        Page<Npc> npcPage = npcQueryDslRepository.searchNpcs(request, pageable);
        return npcPage.map(NpcSummaryDto::toDto);
    }
}