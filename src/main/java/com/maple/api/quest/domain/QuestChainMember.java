package com.maple.api.quest.domain;

import com.maple.api.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quest_chain_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class QuestChainMember extends BaseEntity {
    @Id
    private Integer id;

    @Column(name = "chain_id")
    private Integer chainId;

    @Column(name = "quest_id")
    private Integer questId;

    @Column(name = "sequence_order")
    private Integer sequenceOrder;
}