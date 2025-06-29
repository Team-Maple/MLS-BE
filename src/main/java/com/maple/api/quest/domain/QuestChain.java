package com.maple.api.quest.domain;

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
@Table(name = "quest_chains")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class QuestChain extends BaseEntity {
    @Id
    @Column(name = "chain_id")
    private Integer chainId;

    @Column(name = "chain_name")
    private String chainName;
}