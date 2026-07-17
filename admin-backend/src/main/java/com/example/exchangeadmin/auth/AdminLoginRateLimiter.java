package com.example.exchangeadmin.auth;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminLoginRateLimiter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final long capacity;
    private final long windowSeconds;

    public AdminLoginRateLimiter(
            @Value("${app.rate-limit.login.capacity}") long capacity,
            @Value("${app.rate-limit.login.window-seconds}") long windowSeconds
    ) {
        this.capacity = capacity;
        this.windowSeconds = windowSeconds;
    }

    public ConsumptionProbe tryConsume(String ip, String username) {
        String key = ip + ":" + (username == null ? "" : username.trim().toLowerCase());
        return buckets.computeIfAbsent(key, ignored -> newBucket()).tryConsumeAndReturnRemaining(1);
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.intervally(capacity, Duration.ofSeconds(windowSeconds))
        );
        return Bucket.builder().addLimit(limit).build();
    }
}
