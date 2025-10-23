package com.maple.api.bookmark.repository;

import com.maple.api.bookmark.domain.Bookmark;
import com.maple.api.bookmark.domain.BookmarkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Integer> {
    boolean existsByMemberIdAndBookmarkTypeAndResourceId(String memberId, BookmarkType bookmarkType, Integer resourceId);

    Optional<Bookmark> findByMemberIdAndBookmarkTypeAndResourceId(String memberId, BookmarkType bookmarkType, Integer resourceId);

    @Query("SELECT b.resourceId AS resourceId, b.bookmarkId AS bookmarkId FROM Bookmark b WHERE b.memberId = :memberId AND b.bookmarkType = :bookmarkType AND b.resourceId IN :resourceIds")
    List<BookmarkProjection> findBookmarkIds(
            @Param("memberId") String memberId,
            @Param("bookmarkType") BookmarkType bookmarkType,
            @Param("resourceIds") List<Integer> resourceIds
    );

    interface BookmarkProjection {
        Integer getResourceId();
        Integer getBookmarkId();
    }
}
