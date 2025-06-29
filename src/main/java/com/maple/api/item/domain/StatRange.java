package com.maple.api.item.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatRange {
    
    @Column(name = "base")
    private Integer base;
    
    @Column(name = "min")
    private Integer min;
    
    @Column(name = "max")
    private Integer max;
}