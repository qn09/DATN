package com.example.exchange.wallet.repository;

import com.example.exchange.wallet.entity.WalletBalance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcWalletRepository implements WalletRepository {
    private final JdbcTemplate jdbc;

    public JdbcWalletRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<WalletBalance> find(long accountId, String asset) {
        return jdbc.query(
                """
                        SELECT asset, available, locked
                        FROM wallet_balances
                        WHERE account_id = ? AND asset = ?
                        """,
                (rs, rowNum) -> new WalletBalance(
                        rs.getString("asset"),
                        rs.getBigDecimal("available").stripTrailingZeros(),
                        rs.getBigDecimal("locked").stripTrailingZeros()
                ),
                accountId,
                asset
        ).stream().findFirst();
    }

    @Override
    public List<WalletBalance> findAll(long accountId) {
        return jdbc.query(
                """
                        SELECT asset, available, locked
                        FROM wallet_balances
                        WHERE account_id = ?
                        ORDER BY asset
                        """,
                (rs, rowNum) -> new WalletBalance(
                        rs.getString("asset"),
                        rs.getBigDecimal("available").stripTrailingZeros(),
                        rs.getBigDecimal("locked").stripTrailingZeros()
                ),
                accountId
        );
    }

    @Override
    public void save(long accountId, String asset, BigDecimal available, BigDecimal locked) {
        jdbc.update(
                """
                        INSERT INTO wallet_balances (account_id, asset, available, locked)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT (account_id, asset)
                        DO UPDATE SET
                            available = EXCLUDED.available,
                            locked = EXCLUDED.locked,
                            updated_at = now()
                        """,
                accountId,
                asset,
                available,
                locked
        );
    }

    @Override
    public void clear() {
        jdbc.update("DELETE FROM wallet_balances");
    }
}
