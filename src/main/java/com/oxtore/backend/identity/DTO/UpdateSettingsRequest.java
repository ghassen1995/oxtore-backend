package com.oxtore.backend.identity.DTO;

import jakarta.validation.constraints.Pattern;

/** PATCH /api/users/me/settings. Every field is optional: null leaves the stored value unchanged. */
public record UpdateSettingsRequest(
        @Pattern(regexp = "en|fr|ar", message = "language must be en, fr or ar")
        String language,

        @Pattern(regexp = "[A-Z]{3}", message = "currency must be a 3-letter ISO code, e.g. TND")
        String currency,

        Boolean emailNotifications,
        Boolean pushNotifications
) {}
