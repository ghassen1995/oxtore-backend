package com.oxtore.backend.identity.DTO;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "refreshToken is required")
        String refreshToken
) {
    /** Keeps the token out of DEBUG request logging. */
    @Override
    public String toString() {
        return "RefreshTokenRequest[refreshToken=***]";
    }
}
