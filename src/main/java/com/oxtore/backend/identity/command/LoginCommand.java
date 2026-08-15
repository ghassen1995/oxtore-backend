package com.oxtore.backend.identity.command;


public record LoginCommand(String email, String passwordHash) {
    public LoginCommand {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email cannot be null or empty");
        }

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("password cannot be null or empty");
        }
    }
}
