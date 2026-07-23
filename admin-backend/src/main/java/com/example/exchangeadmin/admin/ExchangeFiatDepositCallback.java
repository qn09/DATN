package com.example.exchangeadmin.admin;

import java.math.BigDecimal;
import java.time.Instant;

public record ExchangeFiatDepositCallback(
        String eventId,
        Instant occurredAt,
        String requestId,
        String gatewayReference,
        String status,
        String currency,
        BigDecimal amount,
        String failureReason
) {
}
