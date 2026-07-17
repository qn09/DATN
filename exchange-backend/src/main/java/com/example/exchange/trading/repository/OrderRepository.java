package com.example.exchange.trading.repository;

import com.example.exchange.trading.entity.Order;
import com.example.exchange.trading.entity.Side;

import java.math.BigDecimal;
import java.util.List;

public interface OrderRepository {
    Order create(long accountId, String symbol, Side side, BigDecimal price, BigDecimal quantity);

    void save(Order order);

    List<Order> findOrders(Long accountId);

    List<Order> findOpenOrders();

    void clear();
}
