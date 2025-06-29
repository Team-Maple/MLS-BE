package com.maple.api.npc.domain;

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
@Table(name = "npcs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Npc extends BaseEntity {
    @Id
    @Column(name = "npc_id")
    private Integer npcId;

    @Column(name = "name_kr")
    private String nameKr;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "icon_url_detail")
    private String iconUrlDetail;
}