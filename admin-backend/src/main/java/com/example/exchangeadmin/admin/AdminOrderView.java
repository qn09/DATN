package com.example.exchangeadmin.admin;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminOrderView(
        long id,
        long accountId,
        String symbol,
        String side,
        BigDecimal price,
        BigDecimal originalQuantity,
        BigDecimal remainingQuantity,
        String status,
        Instant createdAt
) {
}
