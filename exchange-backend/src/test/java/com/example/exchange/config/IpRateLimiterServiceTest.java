package com.example.exchange.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IpRateLimiterServiceTest {
    @Test
    void blocksWhenIpCapacityIsExceeded() {
        IpRateLimiterService limiter = new IpRateLimiterService(2, 60);

        assertThat(limiter.tryConsume("127.0.0.1").isConsumed()).isTrue();
        assertThat(limiter.tryConsume("127.0.0.1").isConsumed()).isTrue();
        assertThat(limiter.tryConsume("127.0.0.1").isConsumed()).isFalse();
        assertThat(limiter.tryConsume("127.0.0.2").isConsumed()).isTrue();
    }
}
