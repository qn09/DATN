package com.example.exchange.trading.entity;

import java.math.BigDecimal;
import java.time.Instant;

public class Order {
    private final long id;
    private final long accountId;
    private final String symbol;
    private final Side side;
    private final BigDecimal price;
    private final BigDecimal originalQuantity;
    private BigDecimal remainingQuantity;
    private OrderStatus status;
    private final Instant createdAt;

    public Order(long id, long accountId, String symbol, Side side, BigDecimal price, BigDecimal quantity) {
        this(id, accountId, symbol, side, price, quantity, quantity, OrderStatus.OPEN, Instant.now());
    }

    public Order(
            long id,
            long accountId,
            String symbol,
            Side side,
            BigDecimal price,
            BigDecimal originalQuantity,
            BigDecimal remainingQuantity,
            OrderStatus status,
            Instant createdAt
    ) {
        this.id = id;
        this.accountId = accountId;
        this.symbol = symbol;
        this.side = side;
        this.price = price;
        this.originalQuantity = originalQuantity;
        this.remainingQuantity = remainingQuantity;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getSymbol() {
        return symbol;
    }

    public Side getSide() {
        return side;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getOriginalQuantity() {
        return originalQuantity;
    }

    public BigDecimal getRemainingQuantity() {
        return remainingQuantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isFilled() {
        return remainingQuantity.signum() == 0;
    }

    public void fill(BigDecimal quantity) {
        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("fill quantity must be positive");
        }
        if (quantity.compareTo(remainingQuantity) > 0) {
            throw new IllegalArgumentException("fill quantity is larger than remaining quantity");
        }

        remainingQuantity = remainingQuantity.subtract(quantity).stripTrailingZeros();
        status = remainingQuantity.signum() == 0 ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
    }
}
