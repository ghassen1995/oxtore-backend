package com.oxtore.backend.identity.exceptions;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String email) {
        super("An account with this email already exists.");
    }
}
