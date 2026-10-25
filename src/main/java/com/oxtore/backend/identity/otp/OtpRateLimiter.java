package com.oxtore.backend.identity.otp;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory sliding-window counters for OTP sends, keyed by normalized email or client IP.
 * Applied before any user lookup, so unknown emails are throttled exactly like real ones. This is per instance and
 * resets on restart; the per-user cooldown and hourly cap are also enforced durably in the database (OtpCodeService).
 */
@Component
public class OtpRateLimiter {

    /** Longest window any caller uses; older events are discarded. */
    private static final Duration RETENTION = Duration.ofHours(1);
    private static final int SWEEP_EVERY = 500;

    private final Map<String, Deque<Instant>> events = new ConcurrentHashMap<>();
    private final AtomicInteger calls = new AtomicInteger();
    private final Clock clock;

    public OtpRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * Seconds to wait before {@code key} may have another event, given at most {@code max} events per {@code window}.
     * 0 when allowed. Does not record anything.
     */
    public long retryAfterSeconds(String key, int max, Duration window) {
        maybeSweep();
        Instant now = clock.instant();
        long[] result = {0};
        events.computeIfPresent(key, (k, q) -> {
            prune(q, now);
            result[0] = retryAfter(q, max, window, now);
            return q.isEmpty() ? null : q;
        });
        return result[0];
    }

    /** Records an event unconditionally. */
    public void record(String key) {
        Instant now = clock.instant();
        events.compute(key, (k, q) -> {
            Deque<Instant> deque = q == null ? new ArrayDeque<>() : q;
            prune(deque, now);
            deque.addLast(now);
            return deque;
        });
    }

    /** Atomically checks and records: returns 0 and records when allowed, otherwise the seconds to wait. */
    public long tryAcquire(String key, int max, Duration window) {
        maybeSweep();
        Instant now = clock.instant();
        long[] result = {0};
        events.compute(key, (k, q) -> {
            Deque<Instant> deque = q == null ? new ArrayDeque<>() : q;
            prune(deque, now);
            result[0] = retryAfter(deque, max, window, now);
            if (result[0] == 0) deque.addLast(now);
            return deque.isEmpty() ? null : deque;
        });
        return result[0];
    }

    private static long retryAfter(Deque<Instant> q, int max, Duration window, Instant now) {
        Instant windowStart = now.minus(window);
        Instant[] inWindow = q.stream().filter(t -> t.isAfter(windowStart)).toArray(Instant[]::new);
        if (inWindow.length < max) return 0;
        // The event that has to leave the window before one more is allowed.
        Instant blocking = inWindow[inWindow.length - max];
        long millis = Duration.between(now, blocking.plus(window)).toMillis();
        return Math.max(1, (millis + 999) / 1000);
    }

    private static void prune(Deque<Instant> q, Instant now) {
        Instant cutoff = now.minus(RETENTION);
        while (!q.isEmpty() && !q.peekFirst().isAfter(cutoff)) q.pollFirst();
    }

    private void maybeSweep() {
        if (calls.incrementAndGet() % SWEEP_EVERY != 0) return;
        Instant now = clock.instant();
        for (String key : events.keySet()) {
            events.computeIfPresent(key, (k, q) -> {
                prune(q, now);
                return q.isEmpty() ? null : q;
            });
        }
    }
}
