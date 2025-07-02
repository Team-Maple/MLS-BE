package com.maple.api.npc.presentation.restapi;

import com.maple.api.npc.application.NpcService;
import com.maple.api.npc.application.dto.NpcSearchRequestDto;
import com.maple.api.npc.application.dto.NpcSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/npcs")
@RequiredArgsConstructor
public class NpcController {

    private final NpcService npcService;

    @GetMapping
    public ResponseEntity<Page<NpcSummaryDto>> searchNpcs(
            @ModelAttribute NpcSearchRequestDto request,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<NpcSummaryDto> npcs = npcService.searchNpcs(request, pageable);
        return ResponseEntity.ok(npcs);
    }
}