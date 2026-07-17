package com.example.exchange.trading.entity;

public record MarketSymbol(String baseAsset, String quoteAsset) {
    public static MarketSymbol parse(String symbol) {
        if (symbol == null || !symbol.contains("-")) {
            throw new IllegalArgumentException("symbol must look like BTC-USDT");
        }

        String[] parts = symbol.trim().toUpperCase().split("-");
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException("symbol must look like BTC-USDT");
        }

        return new MarketSymbol(parts[0], parts[1]);
    }
}
