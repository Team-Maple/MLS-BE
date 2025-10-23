package com.maple.api.npc.application;

import com.maple.api.bookmark.application.BookmarkFlagService;
import com.maple.api.bookmark.domain.BookmarkType;
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
    private final BookmarkFlagService bookmarkFlagService;

    @Transactional(readOnly = true)
    public Page<NpcSummaryDto> searchNpcs(String memberId, NpcSearchRequestDto request, Pageable pageable) {
        Page<Npc> npcPage = npcQueryDslRepository.searchNpcs(request, pageable);
        var ids = npcPage.getContent().stream().map(Npc::getNpcId).toList();
        var bookmarkIds = bookmarkFlagService.findBookmarkIds(memberId, BookmarkType.NPC, ids);
        return npcPage.map(n -> NpcSummaryDto.toDto(n, bookmarkIds.get(n.getNpcId())));
    }

    @Transactional(readOnly = true)
    public NpcDetailDto getNpcDetail(String memberId, Integer npcId) {
        Npc npc = npcRepository.findByNpcId(npcId)
                .orElseThrow(() -> ApiException.of(NpcException.NPC_NOT_FOUND));
        
        Integer bookmarkId = bookmarkFlagService.findBookmarkId(memberId, BookmarkType.NPC, npcId);
        return NpcDetailDto.toDto(npc, bookmarkId);
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
