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
        Map<String, Integer> bookmarkIdMap = buildBookmarkIdMap(memberId, content);

        return resultPage.map(e -> SearchSummaryDto.toDto(e,
                bookmarkIdMap.get(BookmarkedKey.of(e.getType(), e.getOriginalId()))));
    }

    private Map<String, Integer> buildBookmarkIdMap(String memberId, List<VwSearchSummary> content) {
        if (memberId == null || content.isEmpty()) return Collections.emptyMap();

        Map<BookmarkType, List<Integer>> idsByType = content.stream()
                .collect(Collectors.groupingBy(VwSearchSummary::getType,
                        Collectors.mapping(VwSearchSummary::getOriginalId, Collectors.toList())));

        Map<String, Integer> result = new HashMap<>();

        idsByType.forEach((type, ids) -> {
            if (type == null) {
                return;
            }
            Map<Integer, Integer> bookmarkIds = bookmarkFlagService.findBookmarkIds(memberId, type, ids);
            bookmarkIds.forEach((resourceId, bookmarkId) ->
                    result.put(BookmarkedKey.of(type, resourceId), bookmarkId));
        });

        return result;
    }

    private static final class BookmarkedKey {
        private static String of(BookmarkType type, Integer resourceId) {
            if (type == null || resourceId == null) {
                return null;
            }
            return type.name() + ":" + resourceId;
        }
    }
}
