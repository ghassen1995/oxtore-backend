package com.oxtore.backend.identity.DTO;

/** language/currency are null until the user saves one, so the app keeps its device choice. */
public record UserSettingsResponse(String language, String currency, boolean emailNotifications,
                                   boolean pushNotifications) {}
