package com.example.exchange.market.dto;

import java.math.BigDecimal;

public record BinanceTickerPriceResponse(String symbol, BigDecimal price) {
}
