package com.maple.api.bookmark.domain;

import com.maple.api.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bookmark_collections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BookmarkCollection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmark_collection_id")
    private Integer bookmarkCollectionId;

    @Column(name = "bookmark_id", nullable = false)
    private Integer bookmarkId;

    @Column(name = "collection_id", nullable = false)
    private Integer collectionId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public BookmarkCollection(Integer bookmarkId, Integer collectionId, Integer sortOrder) {
        this.bookmarkId = bookmarkId;
        this.collectionId = collectionId;
        this.sortOrder = sortOrder;
    }
}