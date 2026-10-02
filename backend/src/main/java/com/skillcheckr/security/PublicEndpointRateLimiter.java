package com.skillcheckr.security;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Bounded, per-process fixed-window limiter for anonymous authentication endpoints.
 * Production deployments with multiple replicas should also enforce limits at the ingress.
 */
@Component
public class PublicEndpointRateLimiter {

    private static final int MAX_TRACKED_KEYS = 10_000;
    private static final int PRUNE_EVERY_REQUESTS = 256;

    private final Map<String, Window> windows = new LinkedHashMap<>(128, 0.75f, true);
    private final int loginRequests;
    private final long loginWindowSeconds;
    private final int registrationRequests;
    private final long registrationWindowSeconds;
    private int requestsSincePrune;

    public PublicEndpointRateLimiter(
            @Value("${app.security.rate-limit.login.requests:10}") int loginRequests,
            @Value("${app.security.rate-limit.login.window-seconds:60}") long loginWindowSeconds,
            @Value("${app.security.rate-limit.registration.requests:5}") int registrationRequests,
            @Value("${app.security.rate-limit.registration.window-seconds:3600}") long registrationWindowSeconds) {
        this.loginRequests = Math.max(1, loginRequests);
        this.loginWindowSeconds = Math.max(1, loginWindowSeconds);
        this.registrationRequests = Math.max(1, registrationRequests);
        this.registrationWindowSeconds = Math.max(1, registrationWindowSeconds);
    }

    /**
     * @return seconds until the current window resets, or zero when the request is allowed
     */
    public synchronized long tryConsume(String path, String clientAddress) {
        long now = System.nanoTime();
        if (++requestsSincePrune >= PRUNE_EVERY_REQUESTS) {
            pruneExpired(now);
            requestsSincePrune = 0;
        }

        boolean login = "/api/auth/login".equals(path);
        int limit = login ? loginRequests : registrationRequests;
        long durationSeconds = login ? loginWindowSeconds : registrationWindowSeconds;
        String key = path + ':' + (clientAddress == null ? "unknown" : clientAddress);
        long durationNanos = durationSeconds * 1_000_000_000L;
        Window window = windows.get(key);

        if (window == null || now - window.startedAtNanos >= durationNanos) {
            if (window == null && windows.size() >= MAX_TRACKED_KEYS) {
                removeLeastRecentlyUsed();
            }
            windows.put(key, new Window(now, 1));
            return 0;
        }

        if (window.requests >= limit) {
            long remainingNanos = durationNanos - (now - window.startedAtNanos);
            return Math.max(1, (remainingNanos + 999_999_999L) / 1_000_000_000L);
        }

        window.requests++;
        return 0;
    }

    private void pruneExpired(long now) {
        Iterator<Map.Entry<String, Window>> iterator = windows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Window> entry = iterator.next();
            long durationSeconds = entry.getKey().startsWith("/api/auth/login:")
                    ? loginWindowSeconds
                    : registrationWindowSeconds;
            if (now - entry.getValue().startedAtNanos >= durationSeconds * 1_000_000_000L) {
                iterator.remove();
            }
        }
    }

    private void removeLeastRecentlyUsed() {
        Iterator<String> iterator = windows.keySet().iterator();
        if (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private static final class Window {
        private final long startedAtNanos;
        private int requests;

        private Window(long startedAtNanos, int requests) {
            this.startedAtNanos = startedAtNanos;
            this.requests = requests;
        }
    }
}
