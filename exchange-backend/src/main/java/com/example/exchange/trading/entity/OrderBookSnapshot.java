package com.example.exchange.trading.entity;

import java.util.List;

public record OrderBookSnapshot(String symbol, List<Order> bids, List<Order> asks) {
}
