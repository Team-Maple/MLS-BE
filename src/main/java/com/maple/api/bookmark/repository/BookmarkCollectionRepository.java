package com.maple.api.bookmark.repository;

import com.maple.api.bookmark.domain.BookmarkCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkCollectionRepository extends JpaRepository<BookmarkCollection, Integer> {
    
    boolean existsByBookmarkIdAndCollectionId(Integer bookmarkId, Integer collectionId);
    
    @Query("SELECT MAX(bc.sortOrder) FROM BookmarkCollection bc WHERE bc.collectionId = :collectionId")
    Optional<Integer> findMaxSortOrderByCollectionId(@Param("collectionId") Integer collectionId);
    
    @Query("SELECT bc.bookmarkId FROM BookmarkCollection bc WHERE bc.collectionId = :collectionId AND bc.bookmarkId IN :bookmarkIds")
    List<Integer> findExistingBookmarkIds(@Param("collectionId") Integer collectionId, @Param("bookmarkIds") List<Integer> bookmarkIds);
}