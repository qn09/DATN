package com.example.exchangeadmin.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AdminAccountRepository {
    private final JdbcTemplate jdbc;

    public AdminAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AdminPrincipal> findByUsername(String username) {
        return jdbc.query(
                "SELECT id, username, password_hash, role FROM accounts WHERE username = ?",
                (rs, rowNum) -> new AdminPrincipal(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role")
                ),
                username
        ).stream().findFirst();
    }
}
