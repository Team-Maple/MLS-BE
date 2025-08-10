package com.maple.api.bookmark.repository;

import com.maple.api.bookmark.domain.Bookmark;
import com.maple.api.bookmark.domain.BookmarkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Integer> {
    boolean existsByMemberIdAndBookmarkTypeAndResourceId(String memberId, BookmarkType bookmarkType, Integer resourceId);
}