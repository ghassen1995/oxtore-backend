package com.oxtore.backend.identity.DTO;

public record LoginResponse(
        Long id,
        String email,
        String role,
        String token,
        String message
) {}