package com.oxtore.backend.identity.DTO;

public record ApiError(
        String code,
        String message,
        int status
) {
}