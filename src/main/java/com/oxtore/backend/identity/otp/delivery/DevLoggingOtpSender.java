package com.oxtore.backend.identity.otp.delivery;

import com.oxtore.backend.identity.otp.OtpChannel;
import com.oxtore.backend.identity.otp.OtpPurpose;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * LOCAL DEVELOPMENT ONLY: writes codes to the log instead of sending them. Exists only with the "dev" profile AND
 * otp.dev.log-codes=true, refuses to start next to a "prod" profile, and is used only for channels whose real
 * provider isn't configured.
 */
@Slf4j
@Component
@Profile("dev")
@ConditionalOnProperty(name = "otp.dev.log-codes", havingValue = "true")
public class DevLoggingOtpSender {

    public DevLoggingOtpSender(Environment environment) {
        if (environment.matchesProfiles("prod", "production")) {
            throw new IllegalStateException("otp.dev.log-codes must never be enabled together with a production profile");
        }
        log.warn("DEV OTP sender active: verification codes for unconfigured channels are written to this log.");
    }

    public void send(OtpChannel channel, String destination, String code, OtpPurpose purpose) {
        log.warn("[DEV ONLY] {} {} verification code for {}: {}", channel, purpose, destination, code);
    }
}
