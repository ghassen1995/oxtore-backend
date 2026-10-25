package com.oxtore.backend.identity.DTO;

public record LoginRequest(String email, String password) {

    /** Keeps the password out of DEBUG request logging. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
