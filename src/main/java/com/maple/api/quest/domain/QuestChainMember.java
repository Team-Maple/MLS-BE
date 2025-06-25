package com.maple.api.quest.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quest_chain_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class QuestChainMember {
    @Id
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chain_id")
    private QuestChain chain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id")
    private Quest quest;

    @Column(name = "sequence_order")
    private Integer sequenceOrder;
}