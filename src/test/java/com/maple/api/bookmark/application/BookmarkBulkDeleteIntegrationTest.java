package com.maple.api.bookmark.application;

import com.maple.api.auth.domain.Member;
import com.maple.api.bookmark.application.dto.BookmarkBulkDeleteRequestDto;
import com.maple.api.bookmark.domain.Bookmark;
import com.maple.api.bookmark.domain.BookmarkCollection;
import com.maple.api.bookmark.domain.BookmarkType;
import com.maple.api.bookmark.domain.Collection;
import com.maple.api.bookmark.exception.BookmarkException;
import com.maple.api.bookmark.repository.BookmarkCollectionRepository;
import com.maple.api.bookmark.repository.BookmarkRepository;
import com.maple.api.bookmark.repository.CollectionRepository;
import com.maple.api.common.presentation.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@ActiveProfiles("test")
@Import(BookmarkService.class)
class BookmarkBulkDeleteIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private BookmarkService service;
    @Autowired
    private BookmarkRepository bookmarks;
    @Autowired
    private BookmarkCollectionRepository mappings;
    @Autowired
    private CollectionRepository collections;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Bookmark a;
    private Bookmark b;
    private Bookmark unselected;
    private Bookmark foreign;

    @BeforeEach
    void setUp() {
        entityManager.persist(Member.builder().id("owner").nickname("owner").build());
        entityManager.persist(Member.builder().id("other").nickname("other").build());
        Collection first = entityManager.persist(new Collection("owner", "first"));
        Collection second = entityManager.persist(new Collection("owner", "second"));
        Collection foreignCollection = entityManager.persist(new Collection("other", "foreign"));
        a = entityManager.persist(new Bookmark("owner", BookmarkType.ITEM, 101));
        b = entityManager.persist(new Bookmark("owner", BookmarkType.ITEM, 102));
        unselected = entityManager.persist(new Bookmark("owner", BookmarkType.ITEM, 103));
        foreign = entityManager.persist(new Bookmark("other", BookmarkType.ITEM, 104));
        link(first, a);
        link(second, a);
        link(second, b);
        link(first, unselected);
        link(foreignCollection, foreign);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void deletesSelectedBookmarksAndAllTheirMappingsWhilePreservingCollections() {
        var result = service.deleteBookmarks("owner",
                new BookmarkBulkDeleteRequestDto(List.of(a.getBookmarkId(), b.getBookmarkId())));

        assertThat(result.deletedCount()).isEqualTo(2);
        assertThat(bookmarks.findAll()).extracting(Bookmark::getBookmarkId)
                .containsExactlyInAnyOrder(unselected.getBookmarkId(), foreign.getBookmarkId());
        assertThat(mappings.findAll()).extracting(BookmarkCollection::getBookmarkId)
                .containsExactlyInAnyOrder(unselected.getBookmarkId(), foreign.getBookmarkId());
        assertThat(collections.count()).isEqualTo(3);
    }

    @Test
    void deletesBookmarksThatAreNotInAnyCollection() {
        Bookmark detached = entityManager.persistAndFlush(new Bookmark("owner", BookmarkType.MAP, 999));
        assertThat(service.deleteBookmarks("owner",
                new BookmarkBulkDeleteRequestDto(List.of(detached.getBookmarkId()))).deletedCount()).isEqualTo(1);
        assertThat(mappings.count()).isEqualTo(5);
        assertThat(bookmarks.count()).isEqualTo(4);
    }

    @Test
    void rejectsMixedOwnershipBeforeDeletingAnything() {
        assertRejected(List.of(a.getBookmarkId(), foreign.getBookmarkId()), BookmarkException.ACCESS_DENIED);
    }

    @Test
    void rejectsMissingBookmarkBeforeDeletingAnything() {
        assertRejected(List.of(a.getBookmarkId(), 999999), BookmarkException.BOOKMARK_NOT_FOUND);
    }

    @Test
    void rollsBackMappingRemovalIfBookmarkDeletionFails() {
        // Commit fixtures so the service opens its own transaction and rollback is observable.
        TestTransaction.flagForCommit();
        TestTransaction.end();
        jdbcTemplate.execute("""
                CREATE TABLE bookmark_delete_guard (
                    bookmark_id INTEGER PRIMARY KEY REFERENCES bookmarks(bookmark_id)
                )
                """);
        try {
            jdbcTemplate.update("INSERT INTO bookmark_delete_guard (bookmark_id) VALUES (?)", a.getBookmarkId());
            assertThatThrownBy(() -> service.deleteBookmarks("owner",
                    new BookmarkBulkDeleteRequestDto(List.of(a.getBookmarkId(), b.getBookmarkId()))))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThat(bookmarks.count()).isEqualTo(4);
            assertThat(mappings.count()).isEqualTo(5);
        } finally {
            jdbcTemplate.execute("DROP TABLE bookmark_delete_guard");
            jdbcTemplate.update("DELETE FROM bookmark_collections");
            jdbcTemplate.update("DELETE FROM bookmarks");
            jdbcTemplate.update("DELETE FROM collections");
            jdbcTemplate.update("DELETE FROM member WHERE id IN ('owner', 'other')");
        }
    }

    private void assertRejected(List<Integer> ids, BookmarkException exception) {
        assertThatThrownBy(() -> service.deleteBookmarks("owner", new BookmarkBulkDeleteRequestDto(ids)))
                .isInstanceOf(ApiException.class).hasMessage(exception.getMessage());
        assertThat(bookmarks.count()).isEqualTo(4);
        assertThat(mappings.count()).isEqualTo(5);
    }

    private void link(Collection collection, Bookmark bookmark) {
        entityManager.persist(new BookmarkCollection(bookmark.getBookmarkId(), collection.getCollectionId()));
    }
}
