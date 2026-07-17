package com.example.exchange.auth.service;

import com.example.exchange.auth.entity.Account;
import com.example.exchange.auth.repository.AccountRepository;
import com.example.exchange.auth.repository.InMemoryAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {
    private final AccountRepository accounts;

    public AccountService() {
        this(new InMemoryAccountRepository());
    }

    @Autowired
    public AccountService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public Account create(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }

        return accounts.create(username.trim());
    }

    public Account requireAccount(long accountId) {
        return accounts.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("account not found: " + accountId));
    }

    public List<Account> findAll() {
        return accounts.findAll();
    }

    public void clear() {
        accounts.clear();
    }
}
