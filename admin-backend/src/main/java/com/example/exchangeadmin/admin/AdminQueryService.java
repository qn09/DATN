package com.example.exchangeadmin.admin;

import com.example.exchangeadmin.common.AdminAssetCatalog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AdminQueryService {
    private final JdbcTemplate jdbc;

    public AdminQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AdminSummaryResponse summary() {
        return new AdminSummaryResponse(
                count("SELECT count(*) FROM accounts"),
                count("SELECT count(*) FROM exchange_orders"),
                count("SELECT count(*) FROM exchange_orders WHERE status <> 'FILLED'"),
                count("SELECT count(*) FROM exchange_orders WHERE status = 'FILLED'"),
                count("SELECT count(*) FROM trades"),
                stablecoinLiability(),
                AdminAssetCatalog.SUPPORTED_MARKETS
        );
    }

    public List<AdminAccountView> accounts() {
        return jdbc.query(
                "SELECT id, username, role FROM accounts ORDER BY id DESC",
                (rs, rowNum) -> new AdminAccountView(
                        rs.getLong("id"), rs.getString("username"), rs.getString("role"))
        );
    }

    public List<AdminOrderView> orders() {
        return jdbc.query(
                """
                        SELECT id, account_id, symbol, side, price, original_quantity,
                               remaining_quantity, status, created_at
                        FROM exchange_orders
                        ORDER BY id DESC
                        """,
                (rs, rowNum) -> new AdminOrderView(
                        rs.getLong("id"),
                        rs.getLong("account_id"),
                        rs.getString("symbol"),
                        rs.getString("side"),
                        normalize(rs.getBigDecimal("price")),
                        normalize(rs.getBigDecimal("original_quantity")),
                        normalize(rs.getBigDecimal("remaining_quantity")),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant()
                )
        );
    }

    public List<AdminTradeView> trades(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return jdbc.query(
                """
                        SELECT id, symbol, buy_order_id, sell_order_id, price, quantity, source, occurred_at
                        FROM trades
                        ORDER BY id DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new AdminTradeView(
                        rs.getLong("id"),
                        rs.getString("symbol"),
                        rs.getObject("buy_order_id", Long.class),
                        rs.getObject("sell_order_id", Long.class),
                        normalize(rs.getBigDecimal("price")),
                        normalize(rs.getBigDecimal("quantity")),
                        rs.getString("source"),
                        rs.getTimestamp("occurred_at").toInstant()
                ),
                safeLimit
        );
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private BigDecimal stablecoinLiability() {
        BigDecimal value = jdbc.queryForObject(
                "SELECT COALESCE(SUM(available + locked), 0) FROM wallet_balances WHERE asset = 'USDT'",
                BigDecimal.class
        );
        return normalize(value == null ? BigDecimal.ZERO : value);
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value.stripTrailingZeros();
    }
}
