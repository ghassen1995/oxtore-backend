package com.oxtore.backend.identity.exceptions;

import org.springframework.http.HttpStatus;

/** Token refresh and Google sign-in failures, with a stable code the app maps to a translated message. */
public class AuthException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    private AuthException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static AuthException invalidRefreshToken() {
        return new AuthException("INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED,
                "Your session has expired. Please sign in again.");
    }

    /** GOOGLE_CLIENT_ID not set, or Google's signing keys could not be fetched. */
    public static AuthException googleUnavailable() {
        return new AuthException("GOOGLE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                "Google sign-in is not available right now.");
    }

    public static AuthException invalidGoogleToken() {
        return new AuthException("INVALID_GOOGLE_TOKEN", HttpStatus.UNAUTHORIZED,
                "Google sign-in failed. Please try again.");
    }

    public static AuthException googleEmailNotVerified() {
        return new AuthException("GOOGLE_EMAIL_NOT_VERIFIED", HttpStatus.UNAUTHORIZED,
                "Your Google account's email address is not verified.");
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }
}
