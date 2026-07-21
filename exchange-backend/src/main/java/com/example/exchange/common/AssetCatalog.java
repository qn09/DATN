package com.example.exchange.common;

import java.util.List;
import java.util.Locale;

public final class AssetCatalog {
    public static final String QUOTE_ASSET = "USDT";
    public static final String FIAT_ASSET = "VND";
    public static final List<String> SUPPORTED_ASSETS = List.of(
            FIAT_ASSET,
            "USDT",
            "BTC",
            "ETH",
            "BNB",
            "SOL",
            "XRP",
            "ADA",
            "DOGE"
    );
    public static final List<String> SUPPORTED_MARKETS = List.of(
            "BTC-USDT",
            "ETH-USDT",
            "BNB-USDT",
            "SOL-USDT",
            "XRP-USDT",
            "ADA-USDT",
            "DOGE-USDT"
    );

    private AssetCatalog() {
    }

    public static String normalizeAsset(String asset) {
        if (asset == null || asset.isBlank()) {
            throw new IllegalArgumentException("asset is required");
        }
        return asset.trim().toUpperCase(Locale.ROOT);
    }

    public static String requireSupportedAsset(String asset) {
        String normalized = normalizeAsset(asset);
        if (!SUPPORTED_ASSETS.contains(normalized)) {
            throw new IllegalArgumentException("unsupported asset: " + normalized);
        }
        return normalized;
    }

    public static String normalizeMarketSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        return symbol.trim().toUpperCase(Locale.ROOT).replace("/", "-");
    }

    public static String requireSupportedMarketSymbol(String symbol) {
        String normalized = normalizeMarketSymbol(symbol);
        if (!SUPPORTED_MARKETS.contains(normalized)) {
            throw new IllegalArgumentException("unsupported market symbol: " + normalized);
        }
        return normalized;
    }
}
