package com.oxtore.backend.identity.otp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** OTP rules, bound from otp.* in application.properties. */
@ConfigurationProperties("otp")
public record OtpProperties(
        @DefaultValue("10m") Duration ttl,
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("60s") Duration resendCooldown,
        @DefaultValue("5") int maxSendsPerEmailPerHour,
        @DefaultValue("20") int maxSendsPerIpPerHour,
        String hmacSecret
) {
    @Override
    public String toString() {
        return "OtpProperties[ttl=" + ttl + ", maxAttempts=" + maxAttempts + ", resendCooldown=" + resendCooldown
                + ", maxSendsPerEmailPerHour=" + maxSendsPerEmailPerHour
                + ", maxSendsPerIpPerHour=" + maxSendsPerIpPerHour + ", hmacSecret=***]";
    }
}
