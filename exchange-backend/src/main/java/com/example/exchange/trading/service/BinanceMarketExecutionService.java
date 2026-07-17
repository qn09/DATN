package com.example.exchange.trading.service;

import com.example.exchange.market.dto.MarketPriceResponse;
import com.example.exchange.market.exception.MarketDataUnavailableException;
import com.example.exchange.market.service.MarketPriceService;
import com.example.exchange.trading.entity.MarketSymbol;
import com.example.exchange.trading.entity.Order;
import com.example.exchange.trading.entity.Side;
import com.example.exchange.trading.entity.Trade;
import com.example.exchange.trading.repository.OrderRepository;
import com.example.exchange.trading.repository.TradeRepository;
import com.example.exchange.wallet.service.WalletService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
public class BinanceMarketExecutionService {
    private static final String SOURCE = "BINANCE_SPOT";

    private final MarketPriceService marketPrices;
    private final WalletService wallets;
    private final OrderRepository orders;
    private final TradeRepository trades;

    public BinanceMarketExecutionService(
            MarketPriceService marketPrices,
            WalletService wallets,
            OrderRepository orders,
            TradeRepository trades
    ) {
        this.marketPrices = marketPrices;
        this.wallets = wallets;
        this.orders = orders;
        this.trades = trades;
    }

    public Optional<Trade> executeIfMarketable(Order order) {
        MarketPriceResponse marketPrice = currentMarketPrice(order.getSymbol());
        BigDecimal executionPrice = marketPrice.price();

        if (!isMarketable(order, executionPrice)) {
            return Optional.empty();
        }

        BigDecimal quantity = order.getRemainingQuantity();
        MarketSymbol market = MarketSymbol.parse(order.getSymbol());
        order.fill(quantity);

        if (order.getSide() == Side.BUY) {
            wallets.settleMarketBuy(market, order, executionPrice, quantity);
        } else {
            wallets.settleMarketSell(market, order, executionPrice, quantity);
        }

        orders.save(order);
        return Optional.of(createTrade(order, executionPrice, quantity));
    }

    private MarketPriceResponse currentMarketPrice(String symbol) {
        try {
            return marketPrices.getPrice(symbol);
        } catch (MarketDataUnavailableException exception) {
            throw new IllegalArgumentException("Cannot execute order because live market price is unavailable");
        }
    }

    private boolean isMarketable(Order order, BigDecimal marketPrice) {
        if (order.getSide() == Side.BUY) {
            return order.getPrice().compareTo(marketPrice) >= 0;
        }
        return order.getPrice().compareTo(marketPrice) <= 0;
    }

    private Trade createTrade(Order order, BigDecimal executionPrice, BigDecimal quantity) {
        Long buyOrderId = order.getSide() == Side.BUY ? order.getId() : null;
        Long sellOrderId = order.getSide() == Side.SELL ? order.getId() : null;

        return trades.create(
                order.getSymbol(),
                buyOrderId,
                sellOrderId,
                executionPrice.stripTrailingZeros(),
                quantity.stripTrailingZeros(),
                SOURCE,
                Instant.now()
        );
    }
}
