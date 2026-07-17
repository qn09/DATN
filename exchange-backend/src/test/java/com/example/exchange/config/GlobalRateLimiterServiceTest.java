package com.example.exchange.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalRateLimiterServiceTest {
    @Test
    void blocksWhenGlobalCapacityIsExceeded() {
        GlobalRateLimiterService limiter = new GlobalRateLimiterService(2, 60);

        assertThat(limiter.tryConsume().isConsumed()).isTrue();
        assertThat(limiter.tryConsume().isConsumed()).isTrue();
        assertThat(limiter.tryConsume().isConsumed()).isFalse();
    }
}
