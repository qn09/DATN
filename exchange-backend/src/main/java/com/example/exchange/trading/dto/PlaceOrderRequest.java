package com.example.exchange.trading.dto;

import java.math.BigDecimal;

public record PlaceOrderRequest(long accountId, String symbol, String side, BigDecimal price, BigDecimal quantity) {
}
