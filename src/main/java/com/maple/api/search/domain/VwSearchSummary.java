package com.maple.api.search.domain;

import com.maple.api.bookmark.domain.BookmarkType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Getter
@Table(name = "vw_search_summary")
public class VwSearchSummary {

    @Id
    @Column(name = "original_id")
    private Integer originalId;

    @Column(name = "name")
    private String name;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", columnDefinition = "varchar(255)")
    private BookmarkType type;

    @Column(name = "level")
    private Integer level;
}
