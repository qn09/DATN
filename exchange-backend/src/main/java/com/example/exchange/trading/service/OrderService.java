package com.example.exchange.trading.service;

import com.example.exchange.common.AssetCatalog;
import com.example.exchange.auth.service.AccountService;
import com.example.exchange.trading.dto.PlaceOrderRequest;
import com.example.exchange.trading.dto.PlaceOrderResult;
import com.example.exchange.trading.entity.MarketSymbol;
import com.example.exchange.trading.entity.Order;
import com.example.exchange.trading.entity.OrderBookSnapshot;
import com.example.exchange.trading.entity.Side;
import com.example.exchange.trading.entity.Trade;
import com.example.exchange.trading.repository.InMemoryOrderRepository;
import com.example.exchange.trading.repository.OrderRepository;
import com.example.exchange.wallet.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    private final AccountService accounts;
    private final WalletService wallets;
    private final MatchingEngine matchingEngine;
    private final OrderRepository orders;
    private final BinanceMarketExecutionService marketExecution;

    public OrderService(AccountService accounts, WalletService wallets, MatchingEngine matchingEngine) {
        this(accounts, wallets, matchingEngine, new InMemoryOrderRepository(), null);
    }

    @Autowired
    public OrderService(
            AccountService accounts,
            WalletService wallets,
            MatchingEngine matchingEngine,
            OrderRepository orders,
            BinanceMarketExecutionService marketExecution
    ) {
        this.accounts = accounts;
        this.wallets = wallets;
        this.matchingEngine = matchingEngine;
        this.orders = orders;
        this.marketExecution = marketExecution;
    }

    @Transactional
    public PlaceOrderResult place(PlaceOrderRequest request) {
        validate(request);
        accounts.requireAccount(request.accountId());

        String symbol = AssetCatalog.requireSupportedMarketSymbol(request.symbol());
        Side side = Side.valueOf(request.side().trim().toUpperCase());
        MarketSymbol market = MarketSymbol.parse(symbol);
        wallets.reserveForOrder(request.accountId(), market, side, request.price(), request.quantity());

        Order order = orders.create(
                request.accountId(),
                symbol,
                side,
                request.price().stripTrailingZeros(),
                request.quantity().stripTrailingZeros()
        );

        if (marketExecution != null) {
            Optional<Trade> marketTrade = marketExecution.executeIfMarketable(order);
            if (marketTrade.isPresent()) {
                return new PlaceOrderResult(order, List.of(marketTrade.get()));
            }
            matchingEngine.addOpenOrder(order);
            return new PlaceOrderResult(order, List.of());
        }

        List<Trade> trades = matchingEngine.place(order);
        return new PlaceOrderResult(order, trades);
    }

    public List<Order> findOrders(Long accountId) {
        return orders.findOrders(accountId);
    }

    public OrderBookSnapshot orderBook(String symbol) {
        return matchingEngine.snapshot(symbol.trim().toUpperCase());
    }

    public void clear() {
        orders.clear();
    }

    private void validate(PlaceOrderRequest request) {
        if (request.accountId() <= 0) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (request.symbol() == null || request.symbol().isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (request.side() == null || request.side().isBlank()) {
            throw new IllegalArgumentException("side is required");
        }
        try {
            Side.valueOf(request.side().trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("side must be BUY or SELL");
        }
        if (request.price() == null || request.price().signum() <= 0) {
            throw new IllegalArgumentException("price must be positive");
        }
        if (request.quantity() == null || request.quantity().signum() <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
