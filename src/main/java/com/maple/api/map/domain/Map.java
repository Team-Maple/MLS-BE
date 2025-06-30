package com.maple.api.map.domain;

import com.maple.api.common.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "maps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Map extends BaseEntity {
    @Id
    @Column(name = "map_id")
    private Long mapId;

    @NotBlank
    @Column(name = "name_kr")
    private String nameKr;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "region_name")
    private String regionName;

    @Column(name = "detail_name")
    private String detailName;

    @Column(name = "top_region_name")
    private String topRegionName;

    @Size(max = 255)
    @Column(name = "map_url")
    private String mapUrl;

    @Size(max = 255)
    @Column(name = "icon_url")
    private String iconUrl;
}