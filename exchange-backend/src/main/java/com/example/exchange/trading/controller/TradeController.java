package com.example.exchange.trading.controller;

import com.example.exchange.trading.entity.Trade;
import com.example.exchange.trading.service.MatchingEngine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
public class TradeController {
    private final MatchingEngine matchingEngine;

    public TradeController(MatchingEngine matchingEngine) {
        this.matchingEngine = matchingEngine;
    }

    @GetMapping("/{symbol}")
    public List<Trade> trades(@PathVariable String symbol) {
        return matchingEngine.trades(symbol.trim().toUpperCase());
    }
}
