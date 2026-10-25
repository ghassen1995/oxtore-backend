package com.oxtore.backend.identity.DTO;

import jakarta.validation.constraints.NotBlank;

/** idToken: the "credential" returned by Google Identity Services. */
public record GoogleLoginRequest(
        @NotBlank(message = "idToken is required")
        String idToken
) {
    @Override
    public String toString() {
        return "GoogleLoginRequest[idToken=***]";
    }
}
