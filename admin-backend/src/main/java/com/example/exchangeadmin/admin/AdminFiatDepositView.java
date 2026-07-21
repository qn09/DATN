package com.example.exchangeadmin.admin;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminFiatDepositView(
        String requestId,
        long accountId,
        String username,
        String currency,
        BigDecimal amount,
        String status,
        String gateway,
        String gatewayReference,
        String failureReason,
        Instant createdAt,
        Instant completedAt
) {
}
