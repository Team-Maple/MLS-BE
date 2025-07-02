package com.maple.api.monster.presentation.restapi;

import com.maple.api.monster.application.MonsterService;
import com.maple.api.monster.application.dto.MonsterSearchRequestDto;
import com.maple.api.monster.application.dto.MonsterSummaryDto;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/monsters")
@RequiredArgsConstructor
public class MonsterController {

    private final MonsterService monsterService;

    @GetMapping
    public ResponseEntity<Page<MonsterSummaryDto>> searchMonsters(
            @Valid @ModelAttribute MonsterSearchRequestDto request,
            @PageableDefault(size = 20, sort = "monsterId") Pageable pageable) {
        return ResponseEntity.ok(monsterService.searchMonsters(request, pageable));
    }
}