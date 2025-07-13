package com.maple.api.item.domain;

import com.maple.api.common.domain.BaseEntity;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Category extends BaseEntity {
    @Id
    @Column(name = "category_id")
    private Integer categoryId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name")
    private String name;

    @Nullable
    @Column(name = "parent_category_id")
    private Integer parentCategoryId;

    @Column(name = "category_level")
    private Integer categoryLevel;

    @Nullable
    @Size(max = 255)
    @Column(name = "description")
    private String description;
    
    @Column(name = "ui_display_name")
    private String uiDisplayName;

    @Column(name = "disabled")
    private boolean disabled;
}