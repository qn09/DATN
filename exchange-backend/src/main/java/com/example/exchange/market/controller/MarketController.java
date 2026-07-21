package com.example.exchange.market.controller;

import com.example.exchange.market.dto.MarketDepthResponse;
import com.example.exchange.market.dto.MarketKlineResponse;
import com.example.exchange.market.dto.MarketPriceResponse;
import com.example.exchange.market.service.BinanceDepthStreamService;
import com.example.exchange.market.service.BinanceMarketDataService;
import com.example.exchange.market.service.MarketPriceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/market")
public class MarketController {
    private final MarketPriceService marketPriceService;
    private final BinanceMarketDataService marketDataService;
    private final BinanceDepthStreamService depthStreamService;

    public MarketController(
            MarketPriceService marketPriceService,
            BinanceMarketDataService marketDataService,
            BinanceDepthStreamService depthStreamService
    ) {
        this.marketPriceService = marketPriceService;
        this.marketDataService = marketDataService;
        this.depthStreamService = depthStreamService;
    }

    @GetMapping("/prices")
    public List<MarketPriceResponse> getPopularPrices() {
        return marketPriceService.getPopularPrices();
    }

    @GetMapping("/prices/{symbol}")
    public MarketPriceResponse getPrice(@PathVariable String symbol) {
        return marketPriceService.getPrice(symbol);
    }

    @GetMapping("/klines/{symbol}")
    public List<MarketKlineResponse> getKlines(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "15m") String interval,
            @RequestParam(defaultValue = "300") int limit
    ) {
        return marketDataService.getKlines(symbol, interval, limit);
    }

    @GetMapping("/depth/{symbol}")
    public MarketDepthResponse getDepth(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return depthStreamService.getDepth(symbol, limit);
    }
}
