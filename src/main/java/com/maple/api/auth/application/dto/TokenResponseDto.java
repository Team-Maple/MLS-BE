package com.maple.api.auth.application.dto;

public record TokenResponseDto(String accessToken, String refreshToken) {}