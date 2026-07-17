package com.example.exchange.trading.repository;

import com.example.exchange.trading.entity.Order;
import com.example.exchange.trading.entity.OrderStatus;
import com.example.exchange.trading.entity.Side;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryOrderRepository implements OrderRepository {
    private final AtomicLong idSequence = new AtomicLong(1);
    private final Map<Long, Order> orders = new ConcurrentHashMap<>();

    @Override
    public Order create(long accountId, String symbol, Side side, BigDecimal price, BigDecimal quantity) {
        Order order = new Order(idSequence.getAndIncrement(), accountId, symbol, side, price, quantity);
        orders.put(order.getId(), order);
        return order;
    }

    @Override
    public void save(Order order) {
        orders.put(order.getId(), order);
    }

    @Override
    public List<Order> findOrders(Long accountId) {
        return orders.values().stream()
                .filter(order -> accountId == null || order.getAccountId() == accountId)
                .sorted(Comparator.comparing(Order::getId))
                .toList();
    }

    @Override
    public List<Order> findOpenOrders() {
        return orders.values().stream()
                .filter(order -> order.getStatus() != OrderStatus.FILLED)
                .sorted(Comparator.comparing(Order::getId))
                .toList();
    }

    @Override
    public void clear() {
        orders.clear();
        idSequence.set(1);
    }
}
