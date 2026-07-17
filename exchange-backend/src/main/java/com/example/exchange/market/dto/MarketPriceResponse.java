package com.example.exchange.market.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketPriceResponse(
        String symbol,
        String baseAsset,
        String quoteAsset,
        BigDecimal price,
        String source,
        Instant fetchedAt
) {
}
