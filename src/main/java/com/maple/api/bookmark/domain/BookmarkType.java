package com.maple.api.bookmark.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BookmarkType {
    ITEM,
    MONSTER,
    NPC,
    QUEST,
    MAP;

    @JsonCreator
    public static BookmarkType from(String value) {
        if (value == null) {
            return null;
        }

        for (BookmarkType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown bookmark type: " + value);
    }
}
