package com.example.exchange.market.dto;

import java.math.BigDecimal;

public record MarketDepthLevel(BigDecimal price, BigDecimal quantity) {
}
