package com.oxtore.backend.identity.service;

import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.enums.UserRole;
import com.oxtore.backend.identity.exceptions.OtpException;
import com.oxtore.backend.identity.otp.OtpCodeService;
import com.oxtore.backend.identity.otp.OtpPurpose;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-09T10:00:00Z");

    @Test
    void validCodeChangesPasswordAndInvalidatesOlderRefreshTokens() {
        OtpCodeService codes = mock(OtpCodeService.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User user = new User("ABCD2345", "jane@example.com", encoder.encode("old-password"), "Jane", "", "+216", UserRole.USER);
        when(codes.consume("jane@example.com", "123456", OtpPurpose.PASSWORD_RESET)).thenReturn(user);

        new PasswordResetService(codes, encoder, Clock.fixed(NOW, ZoneOffset.UTC))
                .resetPassword(" Jane@Example.com ", "123456", "new-password-1");

        assertThat(encoder.matches("new-password-1", user.getPasswordHash())).isTrue();
        assertThat(encoder.matches("old-password", user.getPasswordHash())).isFalse();
        assertThat(user.getCredentialsChangedAt()).isEqualTo(NOW);
        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    void wrongCodeAttemptsAreNotRolledBack() throws Exception {
        Method reset = PasswordResetService.class.getMethod("resetPassword", String.class, String.class, String.class);
        var tx = reset.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(tx.noRollbackFor()).contains(OtpException.class);
    }
}
