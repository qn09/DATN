package com.example.exchangeadmin.admin;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminTradeView(
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
