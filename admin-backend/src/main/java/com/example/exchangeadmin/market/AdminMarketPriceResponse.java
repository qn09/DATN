package com.example.exchangeadmin.market;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminMarketPriceResponse(
        String symbol,
        String baseAsset,
        String quoteAsset,
        BigDecimal price,
        String source,
        Instant fetchedAt
) {
}
