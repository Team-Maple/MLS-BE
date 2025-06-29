package com.maple.api.map.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "map_npcs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MapNpc {
    @Id
    private Integer id;

    @Column(name = "map_id")
    private Integer mapId;

    @Column(name = "npc_id")
    private Integer npcId;

    @Column(name = "pos_x_pixel_preview")
    private String posXPixelPreview;

    @Column(name = "pos_y_pixel_preview")
    private String posYPixelPreview;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}