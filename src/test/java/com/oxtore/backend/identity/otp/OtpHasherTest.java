package com.oxtore.backend.identity.otp;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OtpHasherTest {

    private static OtpProperties props(String secret) {
        return new OtpProperties(Duration.ofMinutes(10), 5, Duration.ofSeconds(60), 5, 20, secret);
    }

    @Test
    void refusesMissingOrShortSecret() {
        assertThatThrownBy(() -> new OtpHasher(props(null))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new OtpHasher(props("too-short"))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void hashIsBoundToCodeUserAndKey() {
        OtpHasher hasher = new OtpHasher(props("first-secret-0123456789abcdef0123456"));
        String hash = hasher.hash(1L, OtpPurpose.SIGNUP_VERIFICATION, "123456");

        assertThat(hash).hasSize(64).doesNotContain("123456");
        assertThat(hasher.matches(hash, 1L, OtpPurpose.SIGNUP_VERIFICATION, "123456")).isTrue();
        assertThat(hasher.matches(hash, 1L, OtpPurpose.SIGNUP_VERIFICATION, "123457")).isFalse();
        assertThat(hasher.matches(hash, 2L, OtpPurpose.SIGNUP_VERIFICATION, "123456")).isFalse();

        OtpHasher otherKey = new OtpHasher(props("other-secret-0123456789abcdef0123456"));
        assertThat(otherKey.matches(hash, 1L, OtpPurpose.SIGNUP_VERIFICATION, "123456")).isFalse();
    }
}
