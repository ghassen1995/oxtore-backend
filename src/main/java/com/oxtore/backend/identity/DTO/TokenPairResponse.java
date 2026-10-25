package com.oxtore.backend.identity.DTO;

/** Response of POST /api/auth/refresh. */
public record TokenPairResponse(String accessToken, String refreshToken) {}
