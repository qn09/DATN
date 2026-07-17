package com.example.exchange.ledger.repository;

import com.example.exchange.ledger.entity.EntryDirection;
import com.example.exchange.ledger.entity.LedgerAccountType;
import com.example.exchange.ledger.entity.LedgerEntry;
import com.example.exchange.ledger.entity.LedgerPosting;
import com.example.exchange.ledger.entity.LedgerTransaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcLedgerRepository implements LedgerRepository {
    private final JdbcTemplate jdbc;

    public JdbcLedgerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<LedgerTransaction> findByIdempotencyKey(String idempotencyKey) {
        return jdbc.query(
                "SELECT id FROM ledger_transactions WHERE idempotency_key = ?",
                (rs, rowNum) -> rs.getLong("id"),
                idempotencyKey
        ).stream().findFirst().map(this::findById);
    }

    @Override
    public LedgerTransaction save(
            String referenceType,
            String referenceId,
            String idempotencyKey,
            String description,
            List<LedgerPosting> postings
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO ledger_transactions (
                                reference_type, reference_id, idempotency_key, description
                            )
                            VALUES (?, ?, ?, ?)
                            """,
                    new String[]{"id"}
            );
            statement.setString(1, referenceType);
            statement.setString(2, referenceId);
            statement.setString(3, idempotencyKey);
            statement.setString(4, description);
            return statement;
        }, keyHolder);

        long transactionId = keyHolder.getKey().longValue();
        for (LedgerPosting posting : postings) {
            long ledgerAccountId = requireLedgerAccount(posting);
            jdbc.update(
                    """
                            INSERT INTO ledger_entries (
                                transaction_id, ledger_account_id, direction, amount
                            )
                            VALUES (?, ?, ?, ?)
                            """,
                    transactionId,
                    ledgerAccountId,
                    posting.direction().name(),
                    posting.amount()
            );
        }
        return findById(transactionId);
    }

    @Override
    public List<LedgerTransaction> findByOwnerAccountId(long accountId, int limit) {
        List<Long> transactionIds = jdbc.query(
                """
                        SELECT DISTINCT tx.id
                        FROM ledger_transactions tx
                        JOIN ledger_entries entry ON entry.transaction_id = tx.id
                        JOIN ledger_accounts account ON account.id = entry.ledger_account_id
                        WHERE account.owner_account_id = ?
                        ORDER BY tx.id DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> rs.getLong("id"),
                accountId,
                limit
        );
        return transactionIds.stream().map(this::findById).toList();
    }

    @Override
    public void clear() {
        jdbc.update("DELETE FROM ledger_entries");
        jdbc.update("DELETE FROM ledger_transactions");
        jdbc.update("DELETE FROM ledger_accounts");
    }

    private long requireLedgerAccount(LedgerPosting posting) {
        if (posting.ownerAccountId() == null) {
            jdbc.update(
                    """
                            INSERT INTO ledger_accounts (owner_account_id, asset, account_type)
                            VALUES (NULL, ?, ?)
                            ON CONFLICT (asset, account_type)
                            WHERE owner_account_id IS NULL
                            DO NOTHING
                            """,
                    posting.asset(),
                    posting.accountType().name()
            );
            return jdbc.queryForObject(
                    """
                            SELECT id FROM ledger_accounts
                            WHERE owner_account_id IS NULL AND asset = ? AND account_type = ?
                            """,
                    Long.class,
                    posting.asset(),
                    posting.accountType().name()
            );
        }

        jdbc.update(
                """
                        INSERT INTO ledger_accounts (owner_account_id, asset, account_type)
                        VALUES (?, ?, ?)
                        ON CONFLICT (owner_account_id, asset, account_type)
                        WHERE owner_account_id IS NOT NULL
                        DO NOTHING
                        """,
                posting.ownerAccountId(),
                posting.asset(),
                posting.accountType().name()
        );
        return jdbc.queryForObject(
                """
                        SELECT id FROM ledger_accounts
                        WHERE owner_account_id = ? AND asset = ? AND account_type = ?
                        """,
                Long.class,
                posting.ownerAccountId(),
                posting.asset(),
                posting.accountType().name()
        );
    }

    private LedgerTransaction findById(long transactionId) {
        return jdbc.query(
                """
                        SELECT id, reference_type, reference_id, idempotency_key, description, created_at
                        FROM ledger_transactions
                        WHERE id = ?
                        """,
                (rs, rowNum) -> new LedgerTransaction(
                        rs.getLong("id"),
                        rs.getString("reference_type"),
                        rs.getString("reference_id"),
                        rs.getString("idempotency_key"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toInstant(),
                        findEntries(transactionId)
                ),
                transactionId
        ).stream().findFirst().orElseThrow(() -> new IllegalStateException(
                "ledger transaction not found: " + transactionId
        ));
    }

    private List<LedgerEntry> findEntries(long transactionId) {
        return jdbc.query(
                """
                        SELECT
                            entry.id,
                            entry.ledger_account_id,
                            account.owner_account_id,
                            account.asset,
                            account.account_type,
                            entry.direction,
                            entry.amount,
                            entry.created_at
                        FROM ledger_entries entry
                        JOIN ledger_accounts account ON account.id = entry.ledger_account_id
                        WHERE entry.transaction_id = ?
                        ORDER BY entry.id
                        """,
                (rs, rowNum) -> new LedgerEntry(
                        rs.getLong("id"),
                        rs.getLong("ledger_account_id"),
                        rs.getObject("owner_account_id", Long.class),
                        rs.getString("asset"),
                        LedgerAccountType.valueOf(rs.getString("account_type")),
                        EntryDirection.valueOf(rs.getString("direction")),
                        rs.getBigDecimal("amount").stripTrailingZeros(),
                        rs.getTimestamp("created_at").toInstant()
                ),
                transactionId
        );
    }
}
