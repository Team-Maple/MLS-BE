package com.maple.api.bookmark.repository;

import com.maple.api.bookmark.domain.BookmarkCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookmarkCollectionRepository extends JpaRepository<BookmarkCollection, Integer> {

    @Query("SELECT bc.bookmarkId FROM BookmarkCollection bc WHERE bc.collectionId = :collectionId AND bc.bookmarkId IN :bookmarkIds")
    List<Integer> findExistingBookmarkIds(@Param("collectionId") Integer collectionId, @Param("bookmarkIds") List<Integer> bookmarkIds);
    
    @Query("SELECT bc.collectionId FROM BookmarkCollection bc WHERE bc.bookmarkId = :bookmarkId AND bc.collectionId IN :collectionIds")
    List<Integer> findExistingCollectionIds(@Param("bookmarkId") Integer bookmarkId, @Param("collectionIds") List<Integer> collectionIds);
    
    void deleteByCollectionId(Integer collectionId);

    @Query(value = "WITH ranked AS (\n" +
            "  SELECT\n" +
            "    bc.collection_id       AS collectionId,\n" +
            "    b.bookmark_id          AS bookmarkId,\n" +
            "    v.original_id          AS originalId,\n" +
            "    v.name                 AS name,\n" +
            "    v.image_url            AS imageUrl,\n" +
            "    v.type                 AS type,\n" +
            "    v.level                AS level,\n" +
            "    ROW_NUMBER() OVER (PARTITION BY bc.collection_id ORDER BY bc.created_at DESC, bc.bookmark_collection_id DESC) AS rn\n" +
            "  FROM bookmark_collections bc\n" +
            "  JOIN bookmarks b ON b.bookmark_id = bc.bookmark_id AND b.member_id = :memberId\n" +
            "  JOIN vw_search_summary v ON v.original_id = b.resource_id AND v.type = b.bookmark_type\n" +
            "  WHERE bc.collection_id IN (:collectionIds)\n" +
            ")\n" +
            "SELECT collectionId, bookmarkId, originalId, name, imageUrl, type, level\n" +
            "FROM ranked\n" +
            "WHERE rn <= 4\n" +
            "ORDER BY collectionId ASC, rn ASC",
            nativeQuery = true)
    List<CollectionBookmarkRow> findTopRecentBookmarksByCollections(
            @Param("memberId") String memberId,
            @Param("collectionIds") List<Integer> collectionIds
    );

    interface CollectionBookmarkRow {
        Integer getCollectionId();
        Integer getBookmarkId();
        Integer getOriginalId();
        String getName();
        String getImageUrl();
        String getType();
        Integer getLevel();
    }

    List<BookmarkCollection> findByCollectionIdInAndBookmarkIdIn(List<Integer> collectionIds, List<Integer> bookmarkIds);
}
