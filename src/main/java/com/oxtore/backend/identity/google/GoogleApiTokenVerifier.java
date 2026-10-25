package com.oxtore.backend.identity.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.oxtore.backend.identity.exceptions.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Google's official verifier: checks the RS256 signature against Google's published keys (cached), the audience
 * (GOOGLE_CLIENT_ID), the issuer (accounts.google.com) and expiry.
 */
@Slf4j
@Component
public class GoogleApiTokenVerifier implements GoogleTokenVerifier {

    private final String clientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleApiTokenVerifier(@Value("${google.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.verifier = this.clientId.isEmpty() ? null
                : new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                        .setAudience(List.of(this.clientId))
                        .build();
        log.info("Google sign-in: {}", this.clientId.isEmpty() ? "NOT configured (GOOGLE_CLIENT_ID empty)" : "configured");
    }

    @Override
    public boolean isConfigured() {
        return verifier != null;
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        if (verifier == null) throw AuthException.googleUnavailable();
        GoogleIdToken token;
        try {
            token = GoogleIdToken.parse(GsonFactory.getDefaultInstance(), idToken);
        } catch (IOException | IllegalArgumentException e) {
            throw AuthException.invalidGoogleToken(); // not a JWT at all
        }

        boolean valid;
        try {
            valid = verifier.verify(token);
        } catch (IOException e) {
            log.warn("Could not fetch Google signing keys: {}", e.getMessage());
            throw AuthException.googleUnavailable();
        } catch (GeneralSecurityException e) {
            valid = false;
        }
        if (!valid) throw AuthException.invalidGoogleToken();

        GoogleIdToken.Payload payload = token.getPayload();
        return new GoogleIdentity(
                payload.getSubject(),
                payload.getEmail(),
                Boolean.TRUE.equals(payload.getEmailVerified()),
                (String) payload.get("name"),
                (String) payload.get("given_name"),
                (String) payload.get("family_name"),
                (String) payload.get("picture"));
    }
}
