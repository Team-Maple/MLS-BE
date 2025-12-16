package com.maple.api.bookmark.domain;

import com.maple.api.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookmark_id", insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Bookmark bookmark;

    @Column(name = "collection_id", nullable = false)
    private Integer collectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Collection collection;

    public BookmarkCollection(Integer bookmarkId, Integer collectionId) {
        this.bookmarkId = bookmarkId;
        this.collectionId = collectionId;
    }
}
