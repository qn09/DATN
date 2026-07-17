package com.example.exchange.auth.repository;

import com.example.exchange.auth.entity.Account;
import com.example.exchange.auth.entity.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcAccountRepository implements AccountRepository {
    private final JdbcTemplate jdbc;

    public JdbcAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Account create(String username) {
        return create(username, null, Role.USER);
    }

    @Override
    public Account create(String username, String passwordHash, Role role) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO accounts (username, password_hash, role) VALUES (?, ?, ?)",
                    new String[]{"id"}
            );
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, role.name());
            return statement;
        }, keyHolder);

        return new Account(keyHolder.getKey().longValue(), username, passwordHash, role);
    }

    @Override
    public Account updateCredentialsAndRole(String username, String passwordHash, Role role) {
        jdbc.update(
                """
                        UPDATE accounts
                        SET password_hash = ?, role = ?
                        WHERE lower(username) = lower(?)
                        """,
                passwordHash,
                role.name(),
                username
        );
        return findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("account not found: " + username));
    }

    @Override
    public Optional<Account> findById(long accountId) {
        return jdbc.query(
                "SELECT id, username, password_hash, role FROM accounts WHERE id = ?",
                (rs, rowNum) -> mapAccount(rs),
                accountId
        ).stream().findFirst();
    }

    @Override
    public Optional<Account> findByUsername(String username) {
        return jdbc.query(
                "SELECT id, username, password_hash, role FROM accounts WHERE lower(username) = lower(?) ORDER BY id LIMIT 1",
                (rs, rowNum) -> mapAccount(rs),
                username
        ).stream().findFirst();
    }

    @Override
    public List<Account> findAll() {
        return jdbc.query(
                "SELECT id, username, password_hash, role FROM accounts ORDER BY id",
                (rs, rowNum) -> mapAccount(rs)
        );
    }

    @Override
    public void clear() {
        jdbc.update("DELETE FROM accounts");
    }

    private Account mapAccount(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Account(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                Role.valueOf(rs.getString("role"))
        );
    }
}
