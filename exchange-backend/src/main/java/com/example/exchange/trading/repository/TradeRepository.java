package com.example.exchange.trading.repository;

import com.example.exchange.trading.entity.Trade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface TradeRepository {
    Trade create(String symbol, Long buyOrderId, Long sellOrderId, BigDecimal price, BigDecimal quantity, String source, Instant occurredAt);

    List<Trade> findBySymbol(String symbol);

    void clear();
}
