package com.oxtore.backend.identity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * phone and avatar are optional: null leaves the stored value unchanged.
 * avatar must be a URL returned by POST /api/uploads/images?folder=avatars.
 */
public record CompleteProfileRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        String fullName,

        @Size(max = 30, message = "Phone number is too long")
        String phone,

        String avatar
) {}
