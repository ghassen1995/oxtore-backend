package com.oxtore.backend.identity.google;

/** Claims of a verified Google ID token that sign-in uses. Optional values may be null. */
public record GoogleIdentity(
        String subject,
        String email,
        boolean emailVerified,
        String name,
        String givenName,
        String familyName,
        String picture
) {}
