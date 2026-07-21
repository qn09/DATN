package com.example.exchange.deposit.entity;

import java.math.BigDecimal;
import java.time.Instant;

public record FiatDepositRequest(
        long id,
        String requestId,
        long accountId,
        String currency,
        BigDecimal amount,
        FiatDepositStatus status,
        String gateway,
        String gatewayReference,
        String clientRequestKey,
        String failureReason,
        Instant createdAt,
        Instant processingAt,
        Instant completedAt,
        Instant updatedAt
) {
}
