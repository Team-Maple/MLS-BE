package com.maple.api.search.application;

import com.maple.api.bookmark.application.BookmarkFlagService;
import com.maple.api.bookmark.domain.BookmarkType;
import com.maple.api.search.application.dto.SearchSummaryDto;
import com.maple.api.search.domain.VwSearchSummary;
import com.maple.api.search.repository.VwSearchSummaryQueryDslRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final VwSearchSummaryQueryDslRepository vwSearchSummaryQueryDslRepository;
    private final BookmarkFlagService bookmarkFlagService;

    @Transactional(readOnly = true)
    public Page<SearchSummaryDto> search(String memberId, String keyword, Pageable pageable) {
        Page<VwSearchSummary> resultPage = vwSearchSummaryQueryDslRepository.search(keyword, pageable);

        List<VwSearchSummary> content = resultPage.getContent();
        Map<Integer, Boolean> flagMap = buildBookmarkFlagMap(memberId, content);

        return resultPage.map(e -> SearchSummaryDto.toDto(e, flagMap.getOrDefault(e.getOriginalId(), false)));
    }

    private Map<Integer, Boolean> buildBookmarkFlagMap(String memberId, List<VwSearchSummary> content) {
        if (memberId == null || content.isEmpty()) return Collections.emptyMap();

        Map<String, List<Integer>> idsByType = content.stream()
                .collect(Collectors.groupingBy(VwSearchSummary::getType,
                        Collectors.mapping(VwSearchSummary::getOriginalId, Collectors.toList())));

        Map<Integer, Boolean> result = new HashMap<>();

        idsByType.forEach((typeStr, ids) -> {
            BookmarkType type = switch (typeStr) {
                case "ITEM" -> BookmarkType.ITEM;
                case "MONSTER" -> BookmarkType.MONSTER;
                case "NPC" -> BookmarkType.NPC;
                case "QUEST" -> BookmarkType.QUEST;
                case "MAP" -> BookmarkType.MAP;
                default -> null;
            };

            if (type != null) {
                Set<Integer> bookmarked = bookmarkFlagService.findBookmarkedIds(memberId, type, ids);
                for (Integer id : ids) {
                    result.put(id, bookmarked.contains(id));
                }
            }
        });

        return result;
    }
}
