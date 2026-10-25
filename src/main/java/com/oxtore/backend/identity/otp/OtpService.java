package com.oxtore.backend.identity.otp;

import com.oxtore.backend.identity.DTO.SendOtpResponse;
import com.oxtore.backend.identity.exceptions.OtpException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

/**
 * Entry point for the OTP endpoints. Applies the in-memory per-IP and per-email limits first (identically for
 * known and unknown emails), then delegates to the transactional OtpCodeService.
 */
@Service
public class OtpService {

    private static final Duration HOUR = Duration.ofHours(1);

    private final OtpCodeService codeService;
    private final OtpRateLimiter rateLimiter;
    private final OtpProperties properties;

    public OtpService(OtpCodeService codeService, OtpRateLimiter rateLimiter, OtpProperties properties) {
        this.codeService = codeService;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    /** Sign-up verification code over the chosen channel. */
    public SendOtpResponse send(String email, OtpChannel channel, String clientIp) {
        return send(email, channel, clientIp, OtpPurpose.SIGNUP_VERIFICATION);
    }

    /** Password reset code, always by email (the address the account is known by). */
    public SendOtpResponse sendPasswordReset(String email, String clientIp) {
        return send(email, OtpChannel.EMAIL, clientIp, OtpPurpose.PASSWORD_RESET);
    }

    public void verify(String email, String code) {
        codeService.verifySignup(normalize(email), code);
    }

    private SendOtpResponse send(String email, OtpChannel channel, String clientIp, OtpPurpose purpose) {
        String emailKey = "email:" + purpose + ":" + normalize(email);

        // Every request counts against the IP, including rejected ones; the IP budget is shared by all purposes.
        long ipWait = rateLimiter.tryAcquire("ip:" + clientIp, properties.maxSendsPerIpPerHour(), HOUR);
        if (ipWait > 0) throw OtpException.rateLimited(ipWait);

        long cooldownWait = rateLimiter.retryAfterSeconds(emailKey, 1, properties.resendCooldown());
        if (cooldownWait > 0) throw OtpException.cooldown(cooldownWait);
        long hourlyWait = rateLimiter.retryAfterSeconds(emailKey, properties.maxSendsPerEmailPerHour(), HOUR);
        if (hourlyWait > 0) throw OtpException.rateLimited(hourlyWait);

        codeService.issue(normalize(email), channel, clientIp, purpose);

        // Recorded only after a successful (or silently skipped) send, so a delivery failure doesn't start a cooldown.
        rateLimiter.record(emailKey);
        return new SendOtpResponse(
                (int) properties.ttl().toSeconds(), (int) properties.resendCooldown().toSeconds());
    }

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
