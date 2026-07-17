package com.example.exchange.market.controller;

import com.example.exchange.market.dto.MarketPriceResponse;
import com.example.exchange.market.service.MarketPriceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/market")
public class MarketController {
    private final MarketPriceService marketPriceService;

    public MarketController(MarketPriceService marketPriceService) {
        this.marketPriceService = marketPriceService;
    }

    @GetMapping("/prices")
    public List<MarketPriceResponse> getPopularPrices() {
        return marketPriceService.getPopularPrices();
    }

    @GetMapping("/prices/{symbol}")
    public MarketPriceResponse getPrice(@PathVariable String symbol) {
        return marketPriceService.getPrice(symbol);
    }
}
