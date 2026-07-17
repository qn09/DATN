package com.example.exchangeadmin.market;

import com.example.exchangeadmin.common.AdminAssetCatalog;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;

@Service
public class AdminMarketPriceService {
    private static final String SOURCE = "BINANCE_SPOT";

    private final RestClient restClient;

    public AdminMarketPriceService(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://api.binance.com").build();
    }

    public List<AdminMarketPriceResponse> getPopularPrices() {
        return AdminAssetCatalog.SUPPORTED_MARKETS.stream().map(this::getPrice).toList();
    }

    public AdminMarketPriceResponse getPrice(String symbol) {
        String normalized = AdminAssetCatalog.requireSupportedMarket(symbol);
        try {
            BinanceTickerPriceResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v3/ticker/price")
                            .queryParam("symbol", normalized.replace("-", ""))
                            .build())
                    .retrieve()
                    .body(BinanceTickerPriceResponse.class);
            if (response == null || response.price() == null) {
                throw new AdminMarketDataUnavailableException(
                        "Binance did not return a price for " + normalized, null);
            }
            return new AdminMarketPriceResponse(
                    normalized,
                    normalized.substring(0, normalized.indexOf('-')),
                    AdminAssetCatalog.QUOTE_ASSET,
                    response.price(),
                    SOURCE,
                    Instant.now()
            );
        } catch (RestClientException exception) {
            throw new AdminMarketDataUnavailableException(
                    "Cannot load market price for " + normalized, exception);
        }
    }
}
