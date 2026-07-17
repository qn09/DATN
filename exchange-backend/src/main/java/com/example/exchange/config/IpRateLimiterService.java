package com.example.exchange.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IpRateLimiterService {
    private static final Duration IDLE_BUCKET_TTL = Duration.ofMinutes(10);

    private final Map<String, IpBucket> buckets = new ConcurrentHashMap<>();
    private final long capacity;
    private final long windowSeconds;

    public IpRateLimiterService(
            @Value("${app.rate-limit.ip.capacity:300}") long capacity,
            @Value("${app.rate-limit.ip.window-seconds:60}") long windowSeconds
    ) {
        this.capacity = capacity;
        this.windowSeconds = windowSeconds;
    }

    public ConsumptionProbe tryConsume(String clientIp) {
        cleanupIdleBuckets();
        IpBucket ipBucket = buckets.computeIfAbsent(normalizeIp(clientIp), ignored -> new IpBucket(newBucket()));
        ipBucket.touch();
        return ipBucket.bucket().tryConsumeAndReturnRemaining(1);
    }

    public long capacity() {
        return capacity;
    }

    public long windowSeconds() {
        return windowSeconds;
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, Duration.ofSeconds(windowSeconds))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String normalizeIp(String clientIp) {
        return clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim();
    }

    private void cleanupIdleBuckets() {
        Instant expiredBefore = Instant.now().minus(IDLE_BUCKET_TTL);
        buckets.entrySet().removeIf(entry -> entry.getValue().lastSeen().isBefore(expiredBefore));
    }

    private static class IpBucket {
        private final Bucket bucket;
        private volatile Instant lastSeen;

        private IpBucket(Bucket bucket) {
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
