package com.example.exchange.market.service;

import com.example.exchange.market.dto.MarketDepthLevel;
import com.example.exchange.market.dto.MarketDepthResponse;
import com.example.exchange.market.dto.MarketKlineResponse;
import com.example.exchange.market.exception.MarketDataUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

@Service
public class BinanceMarketDataService {
    private static final String SOURCE = "BINANCE_SPOT";
    private static final Set<String> KLINE_INTERVALS = Set.of(
            "1m", "3m", "5m", "15m", "30m", "1h", "2h", "4h", "6h", "8h", "12h", "1d", "3d", "1w", "1M"
    );
    private static final Duration KLINE_CACHE_TTL = Duration.ofSeconds(2);

    private final RestClient restClient;
    private final ConcurrentHashMap<KlineKey, CachedKlines> klineCache = new ConcurrentHashMap<>();

    public BinanceMarketDataService(
            RestClient.Builder builder,
            @Value("${app.binance.market-data-base-url:https://data-api.binance.vision}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public List<MarketKlineResponse> getKlines(String symbol, String interval, int limit) {
        String normalizedSymbol = MarketPriceService.normalizeSymbol(symbol);
        String normalizedInterval = requireInterval(interval);
        int safeLimit = Math.min(Math.max(limit, 1), 1000);
        KlineKey key = new KlineKey(normalizedSymbol, normalizedInterval, safeLimit);
        CachedKlines cached = klineCache.get(key);
        if (cached != null && cached.fetchedAt().plus(KLINE_CACHE_TTL).isAfter(Instant.now())) {
            return cached.klines();
        }

        List<MarketKlineResponse> loaded = loadKlines(normalizedSymbol, normalizedInterval, safeLimit);
        klineCache.put(key, new CachedKlines(loaded, Instant.now()));
        return loaded;
    }

    public MarketDepthResponse getDepthSnapshot(String symbol, int limit) {
        String normalizedSymbol = MarketPriceService.normalizeSymbol(symbol);
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v3/depth")
                            .queryParam("symbol", MarketPriceService.toBinanceSymbol(normalizedSymbol))
                            .queryParam("limit", safeLimit)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.has("lastUpdateId")) {
                throw new MarketDataUnavailableException("Binance did not return depth for " + normalizedSymbol, null);
            }
            return new MarketDepthResponse(
                    normalizedSymbol,
                    response.path("lastUpdateId").asLong(),
                    parseDepthLevels(response.path("bids")),
                    parseDepthLevels(response.path("asks")),
                    SOURCE + "_REST",
                    Instant.now()
            );
        } catch (RestClientException exception) {
            throw new MarketDataUnavailableException("Cannot load market depth for " + normalizedSymbol, exception);
        }
    }

    private List<MarketKlineResponse> loadKlines(String symbol, String interval, int limit) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v3/klines")
                            .queryParam("symbol", MarketPriceService.toBinanceSymbol(symbol))
                            .queryParam("interval", interval)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.isArray()) {
                throw new MarketDataUnavailableException("Binance did not return klines for " + symbol, null);
            }
            return StreamSupport.stream(response.spliterator(), false)
                    .map(BinanceMarketDataService::toKline)
                    .toList();
        } catch (RestClientException exception) {
            throw new MarketDataUnavailableException("Cannot load klines for " + symbol, exception);
        }
    }

    static MarketKlineResponse toKline(JsonNode row) {
        if (!row.isArray() || row.size() < 7) {
            throw new MarketDataUnavailableException("Binance returned an invalid kline", null);
        }
        return new MarketKlineResponse(
                row.get(0).asLong(),
                row.get(6).asLong(),
                decimal(row.get(1)),
                decimal(row.get(2)),
                decimal(row.get(3)),
                decimal(row.get(4)),
                decimal(row.get(5))
        );
    }

    static List<MarketDepthLevel> parseDepthLevels(JsonNode rows) {
        if (!rows.isArray()) {
            return List.of();
        }
        return StreamSupport.stream(rows.spliterator(), false)
                .filter(row -> row.isArray() && row.size() >= 2)
                .map(row -> new MarketDepthLevel(decimal(row.get(0)), decimal(row.get(1))))
                .toList();
    }

    static String requireInterval(String interval) {
        if (interval == null || !KLINE_INTERVALS.contains(interval)) {
            throw new IllegalArgumentException("unsupported kline interval: " + interval);
        }
        return interval;
    }

    private static BigDecimal decimal(JsonNode value) {
        return new BigDecimal(value.asText()).stripTrailingZeros();
    }

    private record KlineKey(String symbol, String interval, int limit) {
    }

    private record CachedKlines(List<MarketKlineResponse> klines, Instant fetchedAt) {
    }
}
