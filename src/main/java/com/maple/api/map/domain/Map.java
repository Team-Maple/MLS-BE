package com.maple.api.map.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "maps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Map {
    @Id
    @Column(name = "map_id")
    private Integer mapId;

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

    @Column(name = "map_url")
    private String mapUrl;

    @Column(name = "icon_url")
    private String iconUrl;
}