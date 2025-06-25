package com.maple.api.item.domain;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "parent_category_id")
    private Integer parentCategoryId;

    @Column(name = "category_level")
    private Integer categoryLevel;

    @Column(name = "description")
    private String description;
}