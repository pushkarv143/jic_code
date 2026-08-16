package com.school.sms.service.impl;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-IP send throttling, held in memory.
 *
 * In memory rather than in Redis on purpose: the application runs as a single
 * container on a host with a few hundred megabytes to spare, so a shared cache
 * would be a second service to run and pay for in order to synchronise a counter
 * between one instance and itself. If the backend is ever scaled beyond one
 * replica this becomes per-replica and needs replacing — which is the only
 * reason it is isolated behind this small class rather than inlined.
 *
 * The per-destination limit in {@code OtpServiceImpl} is the one that protects
 * spend, and that one is in the database and therefore exact. This is the
 * coarser net that stops a single caller sweeping many addresses.
 */
@Component
public class OtpRateLimiter {

    private static final Duration WINDOW = Duration.ofHours(1);

    /** Above this many tracked keys, a sweep runs before the next insert. */
    private static final int SWEEP_THRESHOLD = 10_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * @return false when the key has already used its allowance for the current hour
     */
    public boolean tryAcquire(String key, int maxPerWindow) {
        if (key == null || key.isBlank()) {
            // No usable client address — let it through rather than locking out
            // every caller behind a proxy that strips the header. The
            // per-destination limit still applies.
            return true;
        }

        if (windows.size() > SWEEP_THRESHOLD) {
            sweep();
        }

        Instant now = Instant.now();
        Window window = windows.compute(key, (k, existing) ->
                existing == null || existing.isExpired(now) ? new Window(now) : existing);

        return window.count.incrementAndGet() <= maxPerWindow;
    }

    /** Drops windows that have rolled over, so the map cannot grow without bound. */
    private void sweep() {
        Instant now = Instant.now();
        windows.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private static final class Window {
        private final Instant startedAt;
        private final AtomicInteger count = new AtomicInteger();

        private Window(Instant startedAt) {
            this.startedAt = startedAt;
        }

        private boolean isExpired(Instant now) {
            return startedAt.plus(WINDOW).isBefore(now);
        }
    }
}
