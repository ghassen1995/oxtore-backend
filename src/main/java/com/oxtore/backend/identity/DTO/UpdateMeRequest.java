package com.oxtore.backend.identity.DTO;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/users/me. Every field is optional: null leaves the stored value unchanged.
 * avatar must be a URL returned by POST /api/uploads/images?folder=avatars. The email can't be changed here.
 */
public record UpdateMeRequest(
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        @Pattern(regexp = ".*\\S.*", message = "Full name is required")
        String fullName,

        @Size(max = 30, message = "Phone number is too long")
        @Pattern(regexp = ".*\\S.*", message = "Phone number is required")
        String phone,

        String avatar
) {}
