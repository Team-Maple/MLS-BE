package com.maple.api.bookmark.application;

import com.maple.api.auth.domain.Member;
import com.maple.api.bookmark.application.dto.CollectionWithBookmarksDto;
import com.maple.api.bookmark.domain.Bookmark;
import com.maple.api.bookmark.domain.BookmarkCollection;
import com.maple.api.bookmark.domain.BookmarkType;
import com.maple.api.bookmark.domain.Collection;
import com.maple.api.bookmark.exception.BookmarkException;
import com.maple.api.bookmark.repository.CollectionQueryDslRepositoryImpl;
import com.maple.api.common.presentation.config.QueryDslConfig;
import com.maple.api.common.presentation.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@ActiveProfiles("test")
@Import({QueryDslConfig.class, CollectionQueryDslRepositoryImpl.class, CollectionQueryService.class})
class CollectionInclusionIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CollectionQueryService service;

    private final PageRequest page = PageRequest.of(0, 20, Sort.by("name"));
    private int resourceId;

    @BeforeEach
    void setUp() {
        entityManager.persist(Member.builder().id("owner").nickname("owner").build());
        entityManager.persist(Member.builder().id("other").nickname("other").build());
    }

    @Test
    void distinguishesAllPartialAndNoInclusionAcrossEntireCollections() {
        Collection all = collection("owner", "1-all");
        Collection partial = collection("owner", "2-partial");
        Collection none = collection("owner", "3-none");
        Collection empty = collection("owner", "4-empty");
        Collection foreign = collection("other", "0-foreign");
        List<Bookmark> bookmarks = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Bookmark bookmark = bookmark("owner");
            bookmarks.add(bookmark);
            link(all, bookmark);
        }
        link(partial, bookmarks.get(0));
        link(none, bookmarks.get(6));
        link(foreign, bookmark("other"));
        entityManager.flush();
        entityManager.clear();

        List<Integer> selected = bookmarks.subList(0, 3).stream().map(Bookmark::getBookmarkId).toList();
        List<CollectionWithBookmarksDto> result =
                service.getCollectionsWithRecentBookmarks("owner", page, selected).getContent();

        assertThat(result).extracting(CollectionWithBookmarksDto::collectionId)
                .containsExactly(all.getCollectionId(), partial.getCollectionId(),
                        none.getCollectionId(), empty.getCollectionId());
        assertThat(result).extracting(CollectionWithBookmarksDto::includedCount).containsExactly(3, 1, 0, 0);
        assertThat(result).extracting(CollectionWithBookmarksDto::allIncluded)
                .containsExactly(true, false, false, false);
        assertThat(result.getFirst().recentBookmarks()).hasSize(4);
        assertThat(result.getFirst().recentBookmarks()).noneMatch(bookmark -> selected.contains(bookmark.bookmarkId()));
    }

    @Test
    void countsDistinctBookmarksEvenIfExistingMappingsAreDuplicated() {
        Collection collection = collection("owner", "collection");
        Bookmark bookmark = bookmark("owner");
        link(collection, bookmark);
        link(collection, bookmark);
        entityManager.flush();
        entityManager.clear();

        var result = service.getCollectionsWithRecentBookmarks(
                "owner", page, List.of(bookmark.getBookmarkId())).getContent().getFirst();

        assertThat(result.includedCount()).isEqualTo(1);
        assertThat(result.allIncluded()).isTrue();
    }

    @Test
    void omittingSelectionKeepsLegacyValuesAndSorting() {
        collection("owner", "B");
        collection("owner", "A");
        entityManager.flush();
        entityManager.clear();

        var result = service.getCollectionsWithRecentBookmarks("owner", page, null).getContent();

        assertThat(result).extracting(CollectionWithBookmarksDto::name).containsExactly("A", "B");
        assertThat(result).allSatisfy(collection -> {
            assertThat(collection.includedCount()).isNull();
            assertThat(collection.allIncluded()).isNull();
        });
    }

    @Test
    void rejectsBookmarksOwnedByAnotherMember() {
        Bookmark foreign = bookmark("other");
        entityManager.flush();
        assertThatThrownBy(() -> service.getCollectionsWithRecentBookmarks(
                "owner", page, List.of(foreign.getBookmarkId())))
                .isInstanceOf(ApiException.class)
                .hasMessage(BookmarkException.ACCESS_DENIED.getMessage());
    }

    @Test
    void rejectsMissingBookmarksEvenWhenMemberHasNoCollections() {
        assertThatThrownBy(() -> service.getCollectionsWithRecentBookmarks("owner", page, List.of(999999)))
                .isInstanceOf(ApiException.class)
                .hasMessage(BookmarkException.BOOKMARK_NOT_FOUND.getMessage());
    }

    @Test
    void returnsEmptyListWhenSelectionIsValidButMemberHasNoCollections() {
        Bookmark bookmark = bookmark("owner");
        entityManager.flush();
        assertThat(service.getCollectionsWithRecentBookmarks(
                "owner", page, List.of(bookmark.getBookmarkId())).getContent()).isEmpty();
    }

    private Collection collection(String memberId, String name) {
        return entityManager.persist(new Collection(memberId, name));
    }

    private Bookmark bookmark(String memberId) {
        int id = ++resourceId;
        jdbcTemplate.update("INSERT INTO vw_search_summary (original_id, name, type) VALUES (?, ?, 'ITEM')",
                id, "item-" + id);
        return entityManager.persist(new Bookmark(memberId, BookmarkType.ITEM, id));
    }

    private void link(Collection collection, Bookmark bookmark) {
        entityManager.persist(new BookmarkCollection(bookmark.getBookmarkId(), collection.getCollectionId()));
    }
}
