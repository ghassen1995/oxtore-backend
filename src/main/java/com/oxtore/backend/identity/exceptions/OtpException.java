package com.oxtore.backend.identity.exceptions;

import com.oxtore.backend.identity.otp.OtpChannel;
import org.springframework.http.HttpStatus;

/**
 * Every OTP failure the app can show. {@code code} is stable (the app maps it to a translated message);
 * {@code message} is the English fallback. Messages never say whether an account exists.
 */
public class OtpException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final Long retryAfterSeconds;

    private OtpException(String code, HttpStatus status, String message, Long retryAfterSeconds) {
        super(message);
        this.code = code;
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static OtpException invalid() {
        return new OtpException("OTP_INVALID", HttpStatus.BAD_REQUEST, "Invalid verification code.", null);
    }

    public static OtpException expired() {
        return new OtpException("OTP_EXPIRED", HttpStatus.BAD_REQUEST,
                "This code has expired. Please request a new one.", null);
    }

    public static OtpException tooManyAttempts() {
        return new OtpException("OTP_TOO_MANY_ATTEMPTS", HttpStatus.BAD_REQUEST,
                "Too many incorrect attempts. Please request a new code.", null);
    }

    public static OtpException cooldown(long seconds) {
        return new OtpException("OTP_COOLDOWN", HttpStatus.TOO_MANY_REQUESTS,
                "Please wait " + seconds + " seconds before requesting a new code.", seconds);
    }

    public static OtpException rateLimited(Long retryAfterSeconds) {
        return new OtpException("OTP_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS,
                "Too many code requests. Please try again later.", retryAfterSeconds);
    }

    public static OtpException deliveryFailed() {
        return new OtpException("OTP_DELIVERY_FAILED", HttpStatus.SERVICE_UNAVAILABLE,
                "We couldn't send the code right now. Please try again in a moment.", null);
    }

    public static OtpException channelUnavailable(OtpChannel channel) {
        String name = channel == OtpChannel.WHATSAPP ? "WhatsApp" : "email";
        return new OtpException("OTP_CHANNEL_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                "Sending codes by " + name + " is not available right now.", null);
    }

    public static OtpException invalidPhone() {
        return new OtpException("OTP_INVALID_PHONE", HttpStatus.BAD_REQUEST,
                "Your phone number can't receive WhatsApp codes. It must be in international format, e.g. +21612345678.",
                null);
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }
    public Long getRetryAfterSeconds() { return retryAfterSeconds; }
}
