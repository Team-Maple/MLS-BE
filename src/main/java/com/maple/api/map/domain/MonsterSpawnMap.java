package com.maple.api.map.domain;

import com.maple.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "monster_spawn_maps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MonsterSpawnMap extends BaseEntity {
    @Id
    private Integer id;

    @Column(name = "monster_id")
    private Integer monsterId;

    @Column(name = "map_id")
    private Integer mapId;

    @Column(name = "max_spawn_count")
    private Integer maxSpawnCount;
}