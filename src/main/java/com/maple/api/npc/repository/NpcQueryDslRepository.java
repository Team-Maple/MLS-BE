package com.maple.api.npc.repository;

import com.maple.api.npc.application.dto.NpcSearchRequestDto;
import com.maple.api.npc.domain.Npc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NpcQueryDslRepository {
    Page<Npc> searchNpcs(NpcSearchRequestDto request, Pageable pageable);
}