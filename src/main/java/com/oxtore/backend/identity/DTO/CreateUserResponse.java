package com.oxtore.backend.identity.DTO;

public record CreateUserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phoneNumber
) {}