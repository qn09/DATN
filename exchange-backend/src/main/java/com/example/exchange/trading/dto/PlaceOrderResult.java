package com.example.exchange.trading.dto;

import com.example.exchange.trading.entity.Order;
import com.example.exchange.trading.entity.Trade;

import java.util.List;

public record PlaceOrderResult(Order order, List<Trade> trades) {
}
