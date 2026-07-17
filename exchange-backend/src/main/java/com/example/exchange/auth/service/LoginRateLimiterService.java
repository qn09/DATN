package com.example.exchange.auth.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiterService {
    private static final Duration IDLE_BUCKET_TTL = Duration.ofMinutes(10);

    private final Map<String, LoginBucket> buckets = new ConcurrentHashMap<>();
    private final long capacity;
    private final Duration window;

    public LoginRateLimiterService(
            @Value("${app.rate-limit.login.capacity:5}") long capacity,
            @Value("${app.rate-limit.login.window-seconds:60}") long windowSeconds
    ) {
        this.capacity = capacity;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    public ConsumptionProbe tryConsume(String clientIp, String username) {
        cleanupIdleBuckets();
        LoginBucket loginBucket = buckets.computeIfAbsent(key(clientIp, username), ignored -> new LoginBucket(newBucket()));
        loginBucket.touch();
        return loginBucket.bucket().tryConsumeAndReturnRemaining(1);
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, window)
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String key(String clientIp, String username) {
        String normalizedIp = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim();
        String normalizedUsername = username == null || username.isBlank()
                ? "anonymous"
                : username.trim().toLowerCase(Locale.ROOT);
        return normalizedIp + ":" + normalizedUsername;
    }

    private void cleanupIdleBuckets() {
        Instant expiredBefore = Instant.now().minus(IDLE_BUCKET_TTL);
        buckets.entrySet().removeIf(entry -> entry.getValue().lastSeen().isBefore(expiredBefore));
    }

    private static class LoginBucket {
        private final Bucket bucket;
        private volatile Instant lastSeen;

        private LoginBucket(Bucket bucket) {
            this.bucket = bucket;
            this.lastSeen = Instant.now();
        }

        private Bucket bucket() {
            return bucket;
        }

        private Instant lastSeen() {
            return lastSeen;
        }

        private void touch() {
            lastSeen = Instant.now();
        }
    }
}
