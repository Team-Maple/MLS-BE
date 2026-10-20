package com.maple.api.bookmark.application;

import com.maple.api.auth.domain.Member;
import com.maple.api.bookmark.application.dto.BookmarkCollectionBulkRemoveRequestDto;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@ActiveProfiles("test")
@Import(CollectionService.class)
class BookmarkCollectionBulkRemoveIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private CollectionService service;
    @Autowired
    private BookmarkCollectionRepository mappings;
    @Autowired
    private BookmarkRepository bookmarks;
    @Autowired
    private CollectionRepository collections;

    private Collection first;
    private Collection second;
    private Collection untouched;
    private Collection foreignCollection;
    private Bookmark a;
    private Bookmark b;
    private Bookmark unselected;
    private Bookmark foreignBookmark;

    @BeforeEach
    void setUp() {
        entityManager.persist(Member.builder().id("owner").nickname("owner").build());
        entityManager.persist(Member.builder().id("other").nickname("other").build());
        first = entityManager.persist(new Collection("owner", "first"));
        second = entityManager.persist(new Collection("owner", "second"));
        untouched = entityManager.persist(new Collection("owner", "untouched"));
        foreignCollection = entityManager.persist(new Collection("other", "foreign"));
        a = entityManager.persist(new Bookmark("owner", BookmarkType.ITEM, 101));
        b = entityManager.persist(new Bookmark("owner", BookmarkType.ITEM, 102));
        unselected = entityManager.persist(new Bookmark("owner", BookmarkType.ITEM, 103));
        foreignBookmark = entityManager.persist(new Bookmark("other", BookmarkType.ITEM, 104));
        link(first, a);
        link(first, b);
        link(first, unselected);
        link(second, a);
        link(untouched, a);
        link(untouched, b);
        link(foreignCollection, foreignBookmark);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void removesOnlySelectedPairsAndPreservesBookmarksAndOtherCollections() {
        var request = new BookmarkCollectionBulkRemoveRequestDto(
                List.of(first.getCollectionId(), second.getCollectionId()),
                List.of(a.getBookmarkId(), b.getBookmarkId()));

        assertThat(service.removeBookmarksFromCollections("owner", request).removedCount()).isEqualTo(3);
        assertThat(mappings.findAll())
                .extracting(BookmarkCollection::getCollectionId, BookmarkCollection::getBookmarkId)
                .containsExactlyInAnyOrder(
                        tuple(first.getCollectionId(), unselected.getBookmarkId()),
                        tuple(untouched.getCollectionId(), a.getBookmarkId()),
                        tuple(untouched.getCollectionId(), b.getBookmarkId()),
                        tuple(foreignCollection.getCollectionId(), foreignBookmark.getBookmarkId()));
        assertThat(bookmarks.count()).isEqualTo(4);
        assertThat(collections.count()).isEqualTo(4);
        assertThat(service.removeBookmarksFromCollections("owner", request).removedCount()).isZero();
    }

    @Test
    void returnsZeroForExistingResourcesWithoutAMapping() {
        var request = new BookmarkCollectionBulkRemoveRequestDto(
                List.of(second.getCollectionId()), List.of(b.getBookmarkId()));
        assertThat(service.removeBookmarksFromCollections("owner", request).removedCount()).isZero();
        assertThat(mappings.count()).isEqualTo(7);
    }

    @Test
    void rejectsMixedForeignCollectionsBeforeRemovingAnyMapping() {
        assertRejected(new BookmarkCollectionBulkRemoveRequestDto(
                List.of(first.getCollectionId(), foreignCollection.getCollectionId()),
                List.of(a.getBookmarkId())), BookmarkException.ACCESS_DENIED);
    }

    @Test
    void rejectsMixedForeignBookmarksBeforeRemovingAnyMapping() {
        assertRejected(new BookmarkCollectionBulkRemoveRequestDto(
                List.of(first.getCollectionId()),
                List.of(a.getBookmarkId(), foreignBookmark.getBookmarkId())), BookmarkException.ACCESS_DENIED);
    }

    @Test
    void rejectsMissingCollectionsBeforeRemovingAnyMapping() {
        assertRejected(new BookmarkCollectionBulkRemoveRequestDto(
                List.of(first.getCollectionId(), 999999),
                List.of(a.getBookmarkId())), BookmarkException.COLLECTION_NOT_FOUND);
    }

    @Test
    void rejectsMissingBookmarksBeforeRemovingAnyMapping() {
        assertRejected(new BookmarkCollectionBulkRemoveRequestDto(
                List.of(first.getCollectionId()),
                List.of(a.getBookmarkId(), 999999)), BookmarkException.BOOKMARK_NOT_FOUND);
    }

    private void assertRejected(BookmarkCollectionBulkRemoveRequestDto request, BookmarkException exception) {
        assertThatThrownBy(() -> service.removeBookmarksFromCollections("owner", request))
                .isInstanceOf(ApiException.class).hasMessage(exception.getMessage());
        assertThat(mappings.count()).isEqualTo(7);
        assertThat(bookmarks.count()).isEqualTo(4);
    }

    private void link(Collection collection, Bookmark bookmark) {
        entityManager.persist(new BookmarkCollection(bookmark.getBookmarkId(), collection.getCollectionId()));
    }
}
