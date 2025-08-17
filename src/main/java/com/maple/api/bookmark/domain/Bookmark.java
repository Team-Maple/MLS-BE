package com.maple.api.bookmark.domain;

import com.maple.api.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bookmarks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Bookmark extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmark_id")
    private Integer bookmarkId;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "bookmark_type", nullable = false)
    private BookmarkType bookmarkType;

    @Column(name = "resource_id", nullable = false)
    private Integer resourceId;

    public Bookmark(String memberId, BookmarkType bookmarkType, Integer resourceId) {
        this.memberId = memberId;
        this.bookmarkType = bookmarkType;
        this.resourceId = resourceId;
    }
}