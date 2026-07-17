package com.example.exchange.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class GlobalRateLimiterService {
    private final long capacity;
    private final long windowSeconds;
    private final Bucket bucket;

    public GlobalRateLimiterService(
            @Value("${app.rate-limit.global.capacity:20000}") long capacity,
            @Value("${app.rate-limit.global.window-seconds:60}") long windowSeconds
    ) {
        this.capacity = capacity;
        this.windowSeconds = windowSeconds;
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, Duration.ofSeconds(windowSeconds))
                .build();
        this.bucket = Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public ConsumptionProbe tryConsume() {
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    public long capacity() {
        return capacity;
    }

    public long windowSeconds() {
        return windowSeconds;
    }
}
