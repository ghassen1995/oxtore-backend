package com.oxtore.backend.identity.DTO;

public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {}