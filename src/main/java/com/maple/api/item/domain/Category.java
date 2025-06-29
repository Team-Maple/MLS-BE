package com.maple.api.item.domain;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Category {
    @Id
    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "name")
    private String name;

    @Nullable
    @Column(name = "parent_category_id")
    private Integer parentCategoryId;

    @Column(name = "category_level")
    private Integer categoryLevel;

    @Nullable
    @Column(name = "description")
    private String description;
}