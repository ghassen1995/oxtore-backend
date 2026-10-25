package com.oxtore.backend.identity.otp;

import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.enums.UserRole;
import com.oxtore.backend.identity.exceptions.OtpException;
import com.oxtore.backend.identity.otp.delivery.OtpDeliveryException;
import com.oxtore.backend.identity.otp.delivery.OtpSenderRegistry;
import com.oxtore.backend.identity.repository.IdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OtpCodeServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-09T10:00:00Z");
    private static final OtpPurpose PURPOSE = OtpPurpose.SIGNUP_VERIFICATION;

    private final OtpProperties properties = new OtpProperties(
            Duration.ofMinutes(10), 5, Duration.ofSeconds(60), 5, 20, "test-hmac-secret-0123456789abcdef0123");
    private final OtpHasher hasher = new OtpHasher(properties);

    private OtpCodeRepository codes;
    private IdentityRepository users;
    private OtpSenderRegistry senders;
    private OtpCodeService service;
    private User user;

    @BeforeEach
    void setUp() {
        codes = mock(OtpCodeRepository.class);
        users = mock(IdentityRepository.class);
        senders = mock(OtpSenderRegistry.class);
        service = new OtpCodeService(codes, users, hasher, senders, properties, Clock.fixed(NOW, ZoneOffset.UTC));

        user = new User("ABCD2345", "jane@example.com", "hash", "Jane", "", "+216 12 345 678", UserRole.USER);
        user.setId(7L);
        when(users.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(codes.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(7L, PURPOSE)).thenReturn(Optional.empty());
    }

    // ---- issue ----

    @Test
    void issueStoresOnlyAHashAndDeliversTheCode() {
        service.issue("jane@example.com", OtpChannel.EMAIL, "1.2.3.4", PURPOSE);

        ArgumentCaptor<String> sentCode = ArgumentCaptor.forClass(String.class);
        verify(senders).deliver(eq(OtpChannel.EMAIL), eq("jane@example.com"), sentCode.capture(), eq(Duration.ofMinutes(10)), eq(PURPOSE));
        ArgumentCaptor<OtpCode> saved = ArgumentCaptor.forClass(OtpCode.class);
        verify(codes).save(saved.capture());
        verify(codes).invalidateActive(7L, PURPOSE, NOW);

        String code = sentCode.getValue();
        assertThat(code).matches("\\d{6}");
        assertThat(saved.getValue().getCodeHash()).hasSize(64).doesNotContain(code);
        assertThat(hasher.matches(saved.getValue().getCodeHash(), 7L, PURPOSE, code)).isTrue();
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
    }

    @Test
    void issueInsideCooldownIsRejectedWithoutSending() {
        OtpCode previous = code(NOW.minusSeconds(20));
        when(codes.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(7L, PURPOSE)).thenReturn(Optional.of(previous));

        assertThatThrownBy(() -> service.issue("jane@example.com", OtpChannel.EMAIL, "1.2.3.4", PURPOSE))
                .isInstanceOfSatisfying(OtpException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("OTP_COOLDOWN");
                    assertThat(e.getRetryAfterSeconds()).isEqualTo(40L);
                });
        verifyNoInteractions(senders);
        verify(codes, never()).save(any());
    }

    @Test
    void issueForUnknownOrVerifiedEmailDoesNothing() {
        when(users.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());
        service.issue("nobody@example.com", OtpChannel.EMAIL, "1.2.3.4", PURPOSE);

        user.setEmailVerified(true);
        service.issue("jane@example.com", OtpChannel.EMAIL, "1.2.3.4", PURPOSE);

        verifyNoInteractions(senders);
        verify(codes, never()).save(any());
    }

    @Test
    void deliveryFailureSurfacesAsCleanError() {
        doThrow(new OtpDeliveryException("SMTP down")).when(senders).deliver(any(), any(), any(), any(), any());

        assertThatThrownBy(() -> service.issue("jane@example.com", OtpChannel.EMAIL, "1.2.3.4", PURPOSE))
                .isInstanceOfSatisfying(OtpException.class, e -> assertThat(e.getCode()).isEqualTo("OTP_DELIVERY_FAILED"));
    }

    @Test
    void whatsappUsesNormalizedE164Phone() {
        service.issue("jane@example.com", OtpChannel.WHATSAPP, "1.2.3.4", PURPOSE);
        verify(senders).deliver(eq(OtpChannel.WHATSAPP), eq("+21612345678"), anyString(), any(), any());
    }

    @Test
    void whatsappRejectsPhoneWithoutCountryCode() {
        user.setPhoneNumber("12 345 678");
        assertThatThrownBy(() -> service.issue("jane@example.com", OtpChannel.WHATSAPP, "1.2.3.4", PURPOSE))
                .isInstanceOfSatisfying(OtpException.class, e -> assertThat(e.getCode()).isEqualTo("OTP_INVALID_PHONE"));
        verifyNoInteractions(senders);
    }

    // ---- verify ----

    @Test
    void correctCodeVerifiesUserAndConsumesCode() {
        OtpCode active = activeCode("123456", NOW.plusSeconds(300));

        service.verifySignup("jane@example.com", "123456");

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(active.getConsumedAt()).isEqualTo(NOW);
    }

    @Test
    void fifthWrongAttemptInvalidatesTheCode() {
        OtpCode active = activeCode("123456", NOW.plusSeconds(300));

        for (int i = 1; i <= 4; i++) {
            assertThatThrownBy(() -> service.verifySignup("jane@example.com", "000000"))
                    .isInstanceOfSatisfying(OtpException.class, e -> assertThat(e.getCode()).isEqualTo("OTP_INVALID"));
        }
        assertThat(active.getInvalidatedAt()).isNull();

        assertThatThrownBy(() -> service.verifySignup("jane@example.com", "000000"))
                .isInstanceOfSatisfying(OtpException.class, e -> assertThat(e.getCode()).isEqualTo("OTP_TOO_MANY_ATTEMPTS"));
        assertThat(active.getAttempts()).isEqualTo(5);
        assertThat(active.getInvalidatedAt()).isEqualTo(NOW);
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void expiredCodeFails() {
        activeCode("123456", NOW.minusSeconds(1));

        assertThatThrownBy(() -> service.verifySignup("jane@example.com", "123456"))
                .isInstanceOfSatisfying(OtpException.class, e -> assertThat(e.getCode()).isEqualTo("OTP_EXPIRED"));
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void noActiveCodeOrUnknownEmailFailsTheSameWay() {
        when(codes.findFirstByUserIdAndPurposeAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(7L, PURPOSE))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verifySignup("jane@example.com", "123456"))
                .isInstanceOfSatisfying(OtpException.class, e -> assertThat(e.getCode()).isEqualTo("OTP_INVALID"));

        when(users.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verifySignup("nobody@example.com", "123456"))
                .isInstanceOfSatisfying(OtpException.class, e -> assertThat(e.getCode()).isEqualTo("OTP_INVALID"));
    }

    @Test
    void codeCannotBeReusedOnceUserIsVerified() {
        activeCode("123456", NOW.plusSeconds(300));
        service.verifySignup("jane@example.com", "123456");

        assertThatThrownBy(() -> service.verifySignup("jane@example.com", "123456"))
                .isInstanceOfSatisfying(OtpException.class, e -> assertThat(e.getCode()).isEqualTo("OTP_INVALID"));
    }

    // ---- password reset purpose ----

    @Test
    void resetCodeIsIssuedForVerifiedUsersAndConsumedWithoutTouchingVerification() {
        user.setEmailVerified(true);
        when(codes.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(7L, OtpPurpose.PASSWORD_RESET)).thenReturn(Optional.empty());

        service.issue("jane@example.com", OtpChannel.EMAIL, "1.2.3.4", OtpPurpose.PASSWORD_RESET);
        ArgumentCaptor<String> sentCode = ArgumentCaptor.forClass(String.class);
        verify(senders).deliver(eq(OtpChannel.EMAIL), eq("jane@example.com"), sentCode.capture(), any(),
                eq(OtpPurpose.PASSWORD_RESET));

        OtpCode reset = new OtpCode(user, OtpPurpose.PASSWORD_RESET, OtpChannel.EMAIL,
                hasher.hash(7L, OtpPurpose.PASSWORD_RESET, sentCode.getValue()), NOW, NOW.plusSeconds(600), "1.2.3.4");
        when(codes.findFirstByUserIdAndPurposeAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
                7L, OtpPurpose.PASSWORD_RESET)).thenReturn(Optional.of(reset));

        assertThat(service.consume("jane@example.com", sentCode.getValue(), OtpPurpose.PASSWORD_RESET)).isSameAs(user);
        assertThat(reset.getConsumedAt()).isEqualTo(NOW);
    }

    @Test
    void signupCodeDoesNotWorkAsResetCode() {
        activeCode("123456", NOW.plusSeconds(300)); // a SIGNUP_VERIFICATION code
        when(codes.findFirstByUserIdAndPurposeAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
                7L, OtpPurpose.PASSWORD_RESET)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consume("jane@example.com", "123456", OtpPurpose.PASSWORD_RESET))
                .isInstanceOfSatisfying(OtpException.class, e -> assertThat(e.getCode()).isEqualTo("OTP_INVALID"));
    }

    private OtpCode code(Instant createdAt) {
        return new OtpCode(user, PURPOSE, OtpChannel.EMAIL, hasher.hash(7L, PURPOSE, "123456"),
                createdAt, createdAt.plus(Duration.ofMinutes(10)), "1.2.3.4");
    }

    private OtpCode activeCode(String plainCode, Instant expiresAt) {
        OtpCode otp = new OtpCode(user, PURPOSE, OtpChannel.EMAIL, hasher.hash(7L, PURPOSE, plainCode),
                expiresAt.minus(Duration.ofMinutes(10)), expiresAt, "1.2.3.4");
        when(codes.findFirstByUserIdAndPurposeAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(7L, PURPOSE))
                .thenReturn(Optional.of(otp));
        return otp;
    }
}
