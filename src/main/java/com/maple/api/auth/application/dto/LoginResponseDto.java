package com.maple.api.auth.application.dto;

public record LoginResponseDto(String accessToken, String refreshToken, MemberDto member) {}