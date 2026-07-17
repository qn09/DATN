package com.example.exchange.trading.service;

import com.example.exchange.trading.repository.OrderRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class OpenOrderLoader implements ApplicationRunner {
    private final OrderRepository orders;
    private final MatchingEngine matchingEngine;

    public OpenOrderLoader(OrderRepository orders, MatchingEngine matchingEngine) {
        this.orders = orders;
        this.matchingEngine = matchingEngine;
    }

    @Override
    public void run(ApplicationArguments args) {
        orders.findOpenOrders().forEach(matchingEngine::loadOpenOrder);
    }
}
