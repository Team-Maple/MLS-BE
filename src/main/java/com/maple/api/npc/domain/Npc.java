package com.maple.api.npc.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "npcs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Npc {
    @Id
    @Column(name = "npc_id")
    private Integer npcId;

    @Column(name = "name_kr")
    private String nameKr;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "icon_url_detail")
    private String iconUrlDetail;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}