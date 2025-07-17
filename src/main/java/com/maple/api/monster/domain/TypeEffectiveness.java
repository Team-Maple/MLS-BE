package com.maple.api.monster.domain;

import lombok.Getter;

@Getter
public enum TypeEffectiveness {
    WEAK("약점"),
    RESIST("반감"),
    IMMUNE("면역");

    private final String description;

    TypeEffectiveness(String description) {
        this.description = description;
    }
}