package com.example.exchangeadmin.common;

import java.util.List;
import java.util.Locale;

public final class AdminAssetCatalog {
    public static final String QUOTE_ASSET = "USDT";
    public static final List<String> SUPPORTED_MARKETS = List.of(
            "BTC-USDT",
            "ETH-USDT",
            "BNB-USDT",
            "SOL-USDT",
            "XRP-USDT",
            "ADA-USDT",
            "DOGE-USDT"
    );

    private AdminAssetCatalog() {
    }

    public static String requireSupportedMarket(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT).replace("/", "-");
        if (!SUPPORTED_MARKETS.contains(normalized)) {
            throw new IllegalArgumentException("unsupported market symbol: " + normalized);
        }
        return normalized;
    }
}
