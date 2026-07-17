package com.example.exchange.trading.repository;

import com.example.exchange.trading.entity.Trade;
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
public class JdbcTradeRepository implements TradeRepository {
    private final JdbcTemplate jdbc;

    public JdbcTradeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Trade create(String symbol, Long buyOrderId, Long sellOrderId, BigDecimal price, BigDecimal quantity, String source, Instant occurredAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO trades (symbol, buy_order_id, sell_order_id, price, quantity, source, occurred_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            """,
                    new String[]{"id"}
            );
            statement.setString(1, symbol);
            statement.setObject(2, buyOrderId);
            statement.setObject(3, sellOrderId);
            statement.setBigDecimal(4, price);
            statement.setBigDecimal(5, quantity);
            statement.setString(6, source);
            statement.setTimestamp(7, Timestamp.from(occurredAt));
            return statement;
        }, keyHolder);

        return new Trade(keyHolder.getKey().longValue(), symbol, buyOrderId, sellOrderId, price, quantity, source, occurredAt);
    }

    @Override
    public List<Trade> findBySymbol(String symbol) {
        return jdbc.query(
                """
                        SELECT id, symbol, buy_order_id, sell_order_id, price, quantity, source, occurred_at
                        FROM trades
                        WHERE symbol = ?
                        ORDER BY id
                        """,
                (rs, rowNum) -> new Trade(
                        rs.getLong("id"),
                        rs.getString("symbol"),
                        rs.getObject("buy_order_id", Long.class),
                        rs.getObject("sell_order_id", Long.class),
                        rs.getBigDecimal("price").stripTrailingZeros(),
                        rs.getBigDecimal("quantity").stripTrailingZeros(),
                        rs.getString("source"),
                        rs.getTimestamp("occurred_at").toInstant()
                ),
                symbol
        );
    }

    @Override
    public void clear() {
        jdbc.update("DELETE FROM trades");
    }
}
