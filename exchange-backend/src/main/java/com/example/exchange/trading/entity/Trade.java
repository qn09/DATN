package com.example.exchange.trading.entity;

import java.math.BigDecimal;
import java.time.Instant;

public record Trade(
        long id,
        String symbol,
        Long buyOrderId,
        Long sellOrderId,
        BigDecimal price,
        BigDecimal quantity,
        String source,
        Instant occurredAt
) {
}
