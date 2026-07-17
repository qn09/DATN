package com.example.exchange.trading.repository;

import com.example.exchange.trading.entity.Trade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryTradeRepository implements TradeRepository {
    private final AtomicLong idSequence = new AtomicLong(1);
    private final List<Trade> trades = new ArrayList<>();

    @Override
    public Trade create(String symbol, Long buyOrderId, Long sellOrderId, BigDecimal price, BigDecimal quantity, String source, Instant occurredAt) {
        Trade trade = new Trade(idSequence.getAndIncrement(), symbol, buyOrderId, sellOrderId, price, quantity, source, occurredAt);
        trades.add(trade);
        return trade;
    }

    @Override
    public List<Trade> findBySymbol(String symbol) {
        return trades.stream()
                .filter(trade -> trade.symbol().equalsIgnoreCase(symbol))
                .sorted(Comparator.comparing(Trade::id))
                .toList();
    }

    @Override
    public void clear() {
        trades.clear();
        idSequence.set(1);
    }
}
