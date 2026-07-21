package com.example.exchange.market.dto;

import java.math.BigDecimal;

public record MarketKlineResponse(
        long openTime,
        long closeTime,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume
) {
}
