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
}