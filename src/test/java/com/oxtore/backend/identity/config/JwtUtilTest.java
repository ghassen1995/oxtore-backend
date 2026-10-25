package com.oxtore.backend.identity.config;

import com.oxtore.backend.identity.enums.UserRole;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String ACCESS_SECRET = "a".repeat(64);
    private static final String REFRESH_SECRET = "r".repeat(64);

    private static JwtUtil jwt(String accessSecret, String refreshSecret, long refreshExpirationMs) {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "jwtSecret", accessSecret);
        ReflectionTestUtils.setField(util, "jwtExpirationMs", 60_000L);
        ReflectionTestUtils.setField(util, "refreshSecret", refreshSecret);
        ReflectionTestUtils.setField(util, "refreshExpirationMs", refreshExpirationMs);
        return util;
    }

    @Test
    void refreshTokenRoundTrips() {
        JwtUtil util = jwt(ACCESS_SECRET, REFRESH_SECRET, 60_000L);
        String token = util.generateRefreshToken(42L);
        assertThat(util.parseRefreshToken(token).getSubject()).isEqualTo("42");
    }

    @Test
    void refreshAndAccessTokensAreNotInterchangeable() {
        JwtUtil util = jwt(ACCESS_SECRET, REFRESH_SECRET, 60_000L);
        String access = util.generateToken(42L, "a@b.c", UserRole.USER);
        String refresh = util.generateRefreshToken(42L);

        assertThat(util.isValid(refresh)).isFalse(); // JwtAuthFilter would reject it
        assertThatThrownBy(() -> util.parseRefreshToken(access)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredRefreshTokenIsRejected() {
        JwtUtil util = jwt(ACCESS_SECRET, REFRESH_SECRET, -1_000L);
        String token = util.generateRefreshToken(42L);
        assertThatThrownBy(() -> util.parseRefreshToken(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void refreshSecretMustBeLongAndDistinct() {
        assertThatThrownBy(() -> jwt(ACCESS_SECRET, "short", 1L).validateRefreshSecret())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> jwt(ACCESS_SECRET, ACCESS_SECRET, 1L).validateRefreshSecret())
                .isInstanceOf(IllegalStateException.class);
    }
}
