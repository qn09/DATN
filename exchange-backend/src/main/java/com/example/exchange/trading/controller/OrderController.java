package com.example.exchange.trading.controller;

import com.example.exchange.auth.entity.Account;
import com.example.exchange.auth.entity.Role;
import com.example.exchange.auth.service.AccountAuthorizationService;
import com.example.exchange.trading.dto.PlaceOrderRequest;
import com.example.exchange.trading.dto.PlaceOrderResult;
import com.example.exchange.trading.entity.Order;
import com.example.exchange.trading.entity.OrderBookSnapshot;
import com.example.exchange.trading.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService orders;
    private final AccountAuthorizationService authorization;

    public OrderController(OrderService orders, AccountAuthorizationService authorization) {
        this.orders = orders;
        this.authorization = authorization;
    }

    @PostMapping("/orders")
    public PlaceOrderResult placeOrder(@AuthenticationPrincipal Account currentAccount, @RequestBody PlaceOrderRequest request) {
        authorization.requireOwnerOrAdmin(request.accountId(), currentAccount);
        return orders.place(request);
    }

    @GetMapping("/orders")
    public List<Order> orders(@AuthenticationPrincipal Account currentAccount, @RequestParam(required = false) Long accountId) {
        if (accountId != null) {
            authorization.requireOwnerOrAdmin(accountId, currentAccount);
            return orders.findOrders(accountId);
        }
        if (currentAccount.role() == Role.ADMIN) {
            return orders.findOrders(null);
        }
        return orders.findOrders(currentAccount.id());
    }

    @GetMapping("/accounts/{accountId}/orders")
    public List<Order> accountOrders(@AuthenticationPrincipal Account currentAccount, @PathVariable long accountId) {
        authorization.requireOwnerOrAdmin(accountId, currentAccount);
        return orders.findOrders(accountId);
    }

    @GetMapping("/orderbook/{symbol}")
    public OrderBookSnapshot orderBook(@PathVariable String symbol) {
        return orders.orderBook(symbol);
    }
}
