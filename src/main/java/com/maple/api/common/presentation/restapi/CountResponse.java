package com.maple.api.common.presentation.restapi;

public record CountResponse(long counts) {
    public static CountResponse of(long counts) {
        return new CountResponse(counts);
    }
}
