package com.maple.api.item.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("SCROLL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScrollItem extends Item {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private ItemScrollDetail scrollDetail;
}