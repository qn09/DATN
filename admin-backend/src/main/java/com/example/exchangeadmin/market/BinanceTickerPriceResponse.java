package com.example.exchangeadmin.market;

import java.math.BigDecimal;

public record BinanceTickerPriceResponse(String symbol, BigDecimal price) {
}
