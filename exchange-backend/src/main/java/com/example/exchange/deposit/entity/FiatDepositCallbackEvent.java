package com.example.exchange.deposit.entity;

import java.time.Instant;

public record FiatDepositCallbackEvent(
        String eventId,
        String requestId,
        String signature,
        Instant processedAt
) {
}
