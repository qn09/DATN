package com.example.exchange.market.service;

import com.example.exchange.common.AssetCatalog;
import com.example.exchange.market.dto.BinanceTickerPriceResponse;
import com.example.exchange.market.dto.MarketPriceResponse;
import com.example.exchange.market.exception.MarketDataUnavailableException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;

@Service
public class MarketPriceService {
    private static final String SOURCE = "BINANCE_SPOT";

    private final RestClient restClient;

    public MarketPriceService(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://api.binance.com")
                .build();
    }

    public List<MarketPriceResponse> getPopularPrices() {
        return AssetCatalog.SUPPORTED_MARKETS.stream()
                .map(this::getPrice)
                .toList();
    }

    public MarketPriceResponse getPrice(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        String externalSymbol = toBinanceSymbol(normalizedSymbol);

        try {
            BinanceTickerPriceResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v3/ticker/price")
                            .queryParam("symbol", externalSymbol)
                            .build())
                    .retrieve()
                    .body(BinanceTickerPriceResponse.class);

            if (response == null || response.price() == null) {
                throw new MarketDataUnavailableException("Binance did not return a price for " + normalizedSymbol, null);
            }

            return new MarketPriceResponse(
                    normalizedSymbol,
                    normalizedSymbol.substring(0, normalizedSymbol.indexOf('-')),
                    AssetCatalog.QUOTE_ASSET,
                    response.price(),
                    SOURCE,
                    Instant.now()
            );
        } catch (RestClientException exception) {
            throw new MarketDataUnavailableException("Cannot load market price for " + normalizedSymbol, exception);
        }
    }

    static String normalizeSymbol(String symbol) {
        return AssetCatalog.requireSupportedMarketSymbol(symbol);
    }

    static String toBinanceSymbol(String symbol) {
        return normalizeSymbol(symbol).replace("-", "");
    }
}
