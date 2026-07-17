package com.example.exchange.trading.service;

import com.example.exchange.trading.entity.MarketSymbol;
import com.example.exchange.trading.entity.Order;
import com.example.exchange.trading.entity.OrderBookSnapshot;
import com.example.exchange.trading.entity.Side;
import com.example.exchange.trading.entity.Trade;
import com.example.exchange.trading.repository.InMemoryOrderRepository;
import com.example.exchange.trading.repository.InMemoryTradeRepository;
import com.example.exchange.trading.repository.OrderRepository;
import com.example.exchange.trading.repository.TradeRepository;
import com.example.exchange.wallet.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Component
public class MatchingEngine {
    private final Map<String, Book> books = new HashMap<>();
    private final WalletService wallets;
    private final TradeRepository trades;
    private final OrderRepository orders;

    public MatchingEngine(WalletService wallets) {
        this(wallets, new InMemoryTradeRepository(), new InMemoryOrderRepository());
    }

    @Autowired
    public MatchingEngine(WalletService wallets, TradeRepository trades, OrderRepository orders) {
        this.wallets = wallets;
        this.trades = trades;
        this.orders = orders;
    }

    public synchronized List<Trade> place(Order incoming) {
        Book book = books.computeIfAbsent(incoming.getSymbol(), ignored -> new Book());
        List<Trade> newTrades = incoming.getSide() == Side.BUY
                ? matchBuy(incoming, book)
                : matchSell(incoming, book);

        if (!incoming.isFilled()) {
            if (incoming.getSide() == Side.BUY) {
                book.bids.add(incoming);
            } else {
                book.asks.add(incoming);
            }
        }

        return newTrades;
    }

    public synchronized void loadOpenOrder(Order order) {
        addOpenOrder(order);
    }

    public synchronized void addOpenOrder(Order order) {
        Book book = books.computeIfAbsent(order.getSymbol(), ignored -> new Book());
        if (order.getSide() == Side.BUY) {
            book.bids.add(order);
        } else {
            book.asks.add(order);
        }
    }

    public synchronized OrderBookSnapshot snapshot(String symbol) {
        Book book = books.computeIfAbsent(symbol, ignored -> new Book());
        return new OrderBookSnapshot(
                symbol,
                sorted(book.bids, Comparator.comparing(Order::getPrice).reversed().thenComparing(Order::getCreatedAt)),
                sorted(book.asks, Comparator.comparing(Order::getPrice).thenComparing(Order::getCreatedAt))
        );
    }

    public synchronized List<Trade> trades(String symbol) {
        return trades.findBySymbol(symbol);
    }

    public synchronized void clear() {
        books.clear();
        trades.clear();
    }

    private List<Trade> matchBuy(Order buy, Book book) {
        List<Trade> newTrades = new ArrayList<>();
        while (!buy.isFilled() && !book.asks.isEmpty()) {
            Order sell = book.asks.peek();
            if (sell.getPrice().compareTo(buy.getPrice()) > 0) {
                break;
            }

            BigDecimal quantity = buy.getRemainingQuantity().min(sell.getRemainingQuantity());
            BigDecimal tradePrice = sell.getPrice();
            buy.fill(quantity);
            sell.fill(quantity);

            Trade trade = createTrade(buy.getSymbol(), buy.getId(), sell.getId(), tradePrice, quantity);
            settle(trade, buy, sell);
            orders.save(buy);
            orders.save(sell);
            newTrades.add(trade);

            if (sell.isFilled()) {
                book.asks.poll();
            }
        }
        return newTrades;
    }

    private List<Trade> matchSell(Order sell, Book book) {
        List<Trade> newTrades = new ArrayList<>();
        while (!sell.isFilled() && !book.bids.isEmpty()) {
            Order buy = book.bids.peek();
            if (buy.getPrice().compareTo(sell.getPrice()) < 0) {
                break;
            }

            BigDecimal quantity = sell.getRemainingQuantity().min(buy.getRemainingQuantity());
            BigDecimal tradePrice = buy.getPrice();
            sell.fill(quantity);
            buy.fill(quantity);

            Trade trade = createTrade(sell.getSymbol(), buy.getId(), sell.getId(), tradePrice, quantity);
            settle(trade, buy, sell);
            orders.save(buy);
            orders.save(sell);
            newTrades.add(trade);

            if (buy.isFilled()) {
                book.bids.poll();
            }
        }
        return newTrades;
    }

    private void settle(Trade trade, Order buy, Order sell) {
        MarketSymbol market = MarketSymbol.parse(trade.symbol());
        wallets.settleTrade(market, buy, sell, trade.price(), trade.quantity());
    }

    private Trade createTrade(String symbol, long buyOrderId, long sellOrderId, BigDecimal price, BigDecimal quantity) {
            return trades.create(
                symbol,
                buyOrderId,
                sellOrderId,
                price.stripTrailingZeros(),
                quantity.stripTrailingZeros(),
                "INTERNAL",
                Instant.now()
        );
    }

    private List<Order> sorted(PriorityQueue<Order> queue, Comparator<Order> comparator) {
        return queue.stream().sorted(comparator).toList();
    }

    private static class Book {
        private final PriorityQueue<Order> bids = new PriorityQueue<>(
                Comparator.comparing(Order::getPrice).reversed().thenComparing(Order::getCreatedAt)
        );
        private final PriorityQueue<Order> asks = new PriorityQueue<>(
                Comparator.comparing(Order::getPrice).thenComparing(Order::getCreatedAt)
        );
    }
}
