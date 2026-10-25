package com.oxtore.backend.identity.otp.delivery;

import com.oxtore.backend.identity.otp.OtpChannel;
import com.oxtore.backend.identity.otp.OtpPurpose;

import java.time.Duration;

/** Delivers a verification code over one channel. Implementations must never log the code. */
public interface OtpSender {

    OtpChannel channel();

    /** False when the provider's configuration (env variables) is missing; the channel is then unavailable. */
    boolean isConfigured();

    /**
     * @param destination email address or E.164 phone number, depending on the channel
     * @param purpose     selects the wording (account verification vs password reset)
     * @throws OtpDeliveryException when the provider did not accept the message
     */
    void send(String destination, String code, Duration validity, OtpPurpose purpose);
}
