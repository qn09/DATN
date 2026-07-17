package com.example.exchange.auth.repository;

import com.example.exchange.auth.entity.Account;
import com.example.exchange.auth.entity.Role;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Account create(String username);

    Account create(String username, String passwordHash, Role role);

    Account updateCredentialsAndRole(String username, String passwordHash, Role role);

    Optional<Account> findById(long accountId);

    Optional<Account> findByUsername(String username);

    List<Account> findAll();

    void clear();
}
