package com.oxtore.backend.identity.DTO;

public record CreateUserRequest(
        String email,
        String password,
        String firstName,
        String lastName,
        String phoneNumber
) {}