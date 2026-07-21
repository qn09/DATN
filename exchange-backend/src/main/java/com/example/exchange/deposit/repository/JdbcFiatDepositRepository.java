package com.example.exchange.deposit.repository;

import com.example.exchange.deposit.entity.FiatDepositRequest;
import com.example.exchange.deposit.entity.FiatDepositStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcFiatDepositRepository implements FiatDepositRepository {
    private final JdbcTemplate jdbc;

    public JdbcFiatDepositRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public FiatDepositRequest create(
            String requestId,
            long accountId,
            String currency,
            BigDecimal amount,
            String gateway,
            String clientRequestKey
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO fiat_deposit_requests (
                                request_id, account_id, currency, amount, status, gateway, client_request_key
                            ) VALUES (?, ?, ?, ?, 'NEW', ?, ?)
                            """,
                    new String[]{"id"}
            );
            statement.setString(1, requestId);
            statement.setLong(2, accountId);
            statement.setString(3, currency);
            statement.setBigDecimal(4, amount);
            statement.setString(5, gateway);
            statement.setString(6, clientRequestKey);
            return statement;
        }, keyHolder);
        return findById(keyHolder.getKey().longValue());
    }

    @Override
    public Optional<FiatDepositRequest> findByRequestId(String requestId) {
        return query("request_id = ?", requestId).stream().findFirst();
    }

    @Override
    public Optional<FiatDepositRequest> findByRequestIdForUpdate(String requestId) {
        return jdbc.query(
                selectSql() + " WHERE request_id = ? FOR UPDATE",
                this::map,
                requestId
        ).stream().findFirst();
    }

    @Override
    public Optional<FiatDepositRequest> findByClientRequestKey(String clientRequestKey) {
        return query("client_request_key = ?", clientRequestKey).stream().findFirst();
    }

    @Override
    public List<FiatDepositRequest> findByAccountId(long accountId, int limit) {
        return jdbc.query(
                selectSql() + " WHERE account_id = ? ORDER BY id DESC LIMIT ?",
                this::map,
                accountId,
                limit
        );
    }

    @Override
    public FiatDepositRequest markProcessing(String requestId, String gatewayReference) {
        int updated = jdbc.update(
                """
                        UPDATE fiat_deposit_requests
                        SET status = 'PROCESSING', gateway_reference = ?, processing_at = now(), updated_at = now()
                        WHERE request_id = ? AND status = 'NEW'
                        """,
                gatewayReference,
                requestId
        );
        requireUpdated(updated, requestId, FiatDepositStatus.NEW);
        return requireByRequestId(requestId);
    }

    @Override
    public FiatDepositRequest markSuccess(String requestId) {
        int updated = jdbc.update(
                """
                        UPDATE fiat_deposit_requests
                        SET status = 'SUCCESS', completed_at = now(), updated_at = now(), failure_reason = NULL
                        WHERE request_id = ? AND status = 'PROCESSING'
                        """,
                requestId
        );
        requireUpdated(updated, requestId, FiatDepositStatus.PROCESSING);
        return requireByRequestId(requestId);
    }

    @Override
    public FiatDepositRequest markFailed(String requestId, String failureReason) {
        int updated = jdbc.update(
                """
                        UPDATE fiat_deposit_requests
                        SET status = 'FAILED', failure_reason = ?, completed_at = now(), updated_at = now()
                        WHERE request_id = ? AND status = 'PROCESSING'
                        """,
                failureReason,
                requestId
        );
        requireUpdated(updated, requestId, FiatDepositStatus.PROCESSING);
        return requireByRequestId(requestId);
    }

    private FiatDepositRequest findById(long id) {
        return query("id = ?", id).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("fiat deposit not found: " + id));
    }

    private FiatDepositRequest requireByRequestId(String requestId) {
        return findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("fiat deposit not found: " + requestId));
    }

    private List<FiatDepositRequest> query(String condition, Object value) {
        return jdbc.query(selectSql() + " WHERE " + condition, this::map, value);
    }

    private String selectSql() {
        return """
                SELECT id, request_id, account_id, currency, amount, status, gateway,
                       gateway_reference, client_request_key, failure_reason, created_at,
                       processing_at, completed_at, updated_at
                FROM fiat_deposit_requests
                """;
    }

    private FiatDepositRequest map(ResultSet rs, int rowNum) throws SQLException {
        return new FiatDepositRequest(
                rs.getLong("id"),
                rs.getString("request_id"),
                rs.getLong("account_id"),
                rs.getString("currency"),
                rs.getBigDecimal("amount").stripTrailingZeros(),
                FiatDepositStatus.valueOf(rs.getString("status")),
                rs.getString("gateway"),
                rs.getString("gateway_reference"),
                rs.getString("client_request_key"),
                rs.getString("failure_reason"),
                rs.getTimestamp("created_at").toInstant(),
                instant(rs.getTimestamp("processing_at")),
                instant(rs.getTimestamp("completed_at")),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private java.time.Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private void requireUpdated(int updated, String requestId, FiatDepositStatus expectedStatus) {
        if (updated != 1) {
            throw new IllegalArgumentException(
                    "fiat deposit " + requestId + " is not in " + expectedStatus + " status"
            );
        }
    }
}
