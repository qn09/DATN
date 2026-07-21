package com.example.exchange.market.dto;

import java.time.Instant;
import java.util.List;

public record MarketDepthResponse(
        String symbol,
        long lastUpdateId,
        List<MarketDepthLevel> bids,
        List<MarketDepthLevel> asks,
        String source,
        Instant fetchedAt
) {
}
