package com.example.exchange.trading.repository;

import com.example.exchange.trading.entity.Order;
import com.example.exchange.trading.entity.OrderStatus;
import com.example.exchange.trading.entity.Side;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class JdbcOrderRepository implements OrderRepository {
    private final JdbcTemplate jdbc;

    public JdbcOrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Order create(long accountId, String symbol, Side side, BigDecimal price, BigDecimal quantity) {
        Instant createdAt = Instant.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO exchange_orders (
                                account_id, symbol, side, price,
                                original_quantity, remaining_quantity, status, created_at
                            )
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    new String[]{"id"}
            );
            statement.setLong(1, accountId);
            statement.setString(2, symbol);
            statement.setString(3, side.name());
            statement.setBigDecimal(4, price);
            statement.setBigDecimal(5, quantity);
            statement.setBigDecimal(6, quantity);
            statement.setString(7, OrderStatus.OPEN.name());
            statement.setTimestamp(8, Timestamp.from(createdAt));
            return statement;
        }, keyHolder);

        return new Order(
                keyHolder.getKey().longValue(),
                accountId,
                symbol,
                side,
                price,
                quantity,
                quantity,
                OrderStatus.OPEN,
                createdAt
        );
    }

    @Override
    public void save(Order order) {
        jdbc.update(
                """
                        UPDATE exchange_orders
                        SET remaining_quantity = ?, status = ?
                        WHERE id = ?
                        """,
                order.getRemainingQuantity(),
                order.getStatus().name(),
                order.getId()
        );
    }

    @Override
    public List<Order> findOrders(Long accountId) {
        if (accountId == null) {
            return jdbc.query("SELECT * FROM exchange_orders ORDER BY id", this::mapOrder);
        }
        return jdbc.query(
                "SELECT * FROM exchange_orders WHERE account_id = ? ORDER BY id",
                this::mapOrder,
                accountId
        );
    }

    @Override
    public List<Order> findOpenOrders() {
        return jdbc.query(
                """
                        SELECT *
                        FROM exchange_orders
                        WHERE status <> ?
                        ORDER BY id
                        """,
                this::mapOrder,
                OrderStatus.FILLED.name()
        );
    }

    @Override
    public void clear() {
        jdbc.update("DELETE FROM exchange_orders");
    }

    private Order mapOrder(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Order(
                rs.getLong("id"),
                rs.getLong("account_id"),
                rs.getString("symbol"),
                Side.valueOf(rs.getString("side")),
                rs.getBigDecimal("price").stripTrailingZeros(),
                rs.getBigDecimal("original_quantity").stripTrailingZeros(),
                rs.getBigDecimal("remaining_quantity").stripTrailingZeros(),
                OrderStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}
