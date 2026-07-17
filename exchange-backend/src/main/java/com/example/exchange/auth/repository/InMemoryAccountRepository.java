package com.example.exchange.auth.repository;

import com.example.exchange.auth.entity.Account;
import com.example.exchange.auth.entity.Role;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAccountRepository implements AccountRepository {
    private final AtomicLong idSequence = new AtomicLong(1);
    private final Map<Long, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public Account create(String username) {
        return create(username, null, Role.USER);
    }

    @Override
    public Account create(String username, String passwordHash, Role role) {
        long id = idSequence.getAndIncrement();
        Account account = new Account(id, username, passwordHash, role);
        accounts.put(id, account);
        return account;
    }

    @Override
    public Account updateCredentialsAndRole(String username, String passwordHash, Role role) {
        Account current = findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("account not found: " + username));
        Account updated = new Account(current.id(), current.username(), passwordHash, role);
        accounts.put(updated.id(), updated);
        return updated;
    }

    @Override
    public Optional<Account> findById(long accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }

    @Override
    public Optional<Account> findByUsername(String username) {
        return accounts.values().stream()
                .filter(account -> account.username().equalsIgnoreCase(username))
                .findFirst();
    }

    @Override
    public List<Account> findAll() {
        return new ArrayList<>(accounts.values()).stream()
                .sorted(Comparator.comparing(Account::id))
                .toList();
    }

    @Override
    public void clear() {
        accounts.clear();
        idSequence.set(1);
    }
}
