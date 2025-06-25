package com.maple.api.map.domain;

import com.maple.api.npc.domain.Npc;
import jakarta.persistence.*;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_id")
    private Map map;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "npc_id")
    private Npc npc;

    @Column(name = "npc_name")
    private String npcName;

    @Column(name = "npc_icon_url")
    private String npcIconUrl;

    @Column(name = "pos_x_pixel_preview")
    private String posXPixelPreview;

    @Column(name = "pos_y_pixel_preview")
    private String posYPixelPreview;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}