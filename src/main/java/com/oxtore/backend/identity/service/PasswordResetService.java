package com.oxtore.backend.identity.service;

import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.exceptions.OtpException;
import com.oxtore.backend.identity.otp.OtpCodeService;
import com.oxtore.backend.identity.otp.OtpPurpose;
import com.oxtore.backend.identity.otp.OtpService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class PasswordResetService {

    private final OtpCodeService otpCodeService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public PasswordResetService(OtpCodeService otpCodeService, PasswordEncoder passwordEncoder, Clock clock) {
        this.otpCodeService = otpCodeService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Same code rules as sign-up (6 digits, 10 min, 5 attempts, single use). On success the password changes,
     * refresh tokens issued before now stop working, and the email counts as verified (the code proved ownership).
     * noRollbackFor keeps wrong-attempt counts when a code is rejected.
     */
    @Transactional(noRollbackFor = OtpException.class)
    public void resetPassword(String email, String code, String newPassword) {
        User user = otpCodeService.consume(OtpService.normalize(email), code, OtpPurpose.PASSWORD_RESET);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setCredentialsChangedAt(clock.instant());
        user.setEmailVerified(true);
    }
}
