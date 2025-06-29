package com.maple.api.monster.domain;

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
@Table(name = "item_monster_drops")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ItemMonsterDrop extends BaseEntity {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "monster_id")
    private Integer monsterId;

    @Column(name = "drop_rate")
    private Double dropRate;
}