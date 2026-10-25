package com.oxtore.backend.identity.otp;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class OtpRateLimiterTest {

    /** Clock that tests can move forward. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-09T10:00:00Z");

        void advance(Duration d) { now = now.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final MutableClock clock = new MutableClock();
    private final OtpRateLimiter limiter = new OtpRateLimiter(clock);

    @Test
    void cooldownBlocksUntilItElapses() {
        limiter.record("email:a@b.c");
        assertThat(limiter.retryAfterSeconds("email:a@b.c", 1, Duration.ofSeconds(60))).isEqualTo(60);

        clock.advance(Duration.ofSeconds(45));
        assertThat(limiter.retryAfterSeconds("email:a@b.c", 1, Duration.ofSeconds(60))).isEqualTo(15);

        clock.advance(Duration.ofSeconds(15));
        assertThat(limiter.retryAfterSeconds("email:a@b.c", 1, Duration.ofSeconds(60))).isZero();
    }

    @Test
    void hourlyCapCountsOnlyEventsInsideTheWindow() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("ip:1.2.3.4", 3, Duration.ofHours(1))).isZero();
            clock.advance(Duration.ofMinutes(10));
        }
        // 3 events at t=0, 10, 20 min; now t=30 min: blocked until the first leaves the window at t=60.
        assertThat(limiter.tryAcquire("ip:1.2.3.4", 3, Duration.ofHours(1))).isEqualTo(30 * 60);

        clock.advance(Duration.ofMinutes(30));
        assertThat(limiter.tryAcquire("ip:1.2.3.4", 3, Duration.ofHours(1))).isZero();
    }

    @Test
    void keysAreIndependent() {
        limiter.record("email:a@b.c");
        assertThat(limiter.retryAfterSeconds("email:other@b.c", 1, Duration.ofSeconds(60))).isZero();
    }
}
