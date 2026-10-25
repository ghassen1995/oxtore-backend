package com.oxtore.backend.identity.google;

/** Verifies Google Sign-In ID tokens issued for this app's OAuth client. */
public interface GoogleTokenVerifier {

    /** False when GOOGLE_CLIENT_ID is not set. */
    boolean isConfigured();

    /**
     * @throws com.oxtore.backend.identity.exceptions.AuthException INVALID_GOOGLE_TOKEN for a bad, expired or foreign
     *         token; GOOGLE_UNAVAILABLE when Google's keys can't be fetched or the client id is missing
     */
    GoogleIdentity verify(String idToken);
}
