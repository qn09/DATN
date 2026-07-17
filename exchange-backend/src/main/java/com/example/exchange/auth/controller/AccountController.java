package com.example.exchange.auth.controller;

import com.example.exchange.auth.dto.CreateAccountRequest;
import com.example.exchange.wallet.dto.DepositRequest;
import com.example.exchange.wallet.dto.WalletBalanceView;
import com.example.exchange.auth.entity.Account;
import com.example.exchange.auth.service.AccountAuthorizationService;
import com.example.exchange.auth.service.AccountService;
import com.example.exchange.wallet.service.WalletService;
import com.example.exchange.wallet.service.WalletValuationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accounts;
    private final AccountAuthorizationService authorization;
    private final WalletService wallets;
    private final WalletValuationService walletValuations;

    public AccountController(
            AccountService accounts,
            AccountAuthorizationService authorization,
            WalletService wallets,
            WalletValuationService walletValuations
    ) {
        this.accounts = accounts;
        this.authorization = authorization;
        this.wallets = wallets;
        this.walletValuations = walletValuations;
    }

    @PostMapping
    public Account create(@AuthenticationPrincipal Account currentAccount, @RequestBody CreateAccountRequest request) {
        authorization.requireAdmin(currentAccount);
        return accounts.create(request.username());
    }

    @GetMapping
    public List<Account> list(@AuthenticationPrincipal Account currentAccount) {
        if (currentAccount.role().name().equals("ADMIN")) {
            return accounts.findAll();
        }
        return List.of(currentAccount);
    }

    @GetMapping("/{accountId}")
    public Account get(@AuthenticationPrincipal Account currentAccount, @PathVariable long accountId) {
        authorization.requireOwnerOrAdmin(accountId, currentAccount);
        return accounts.requireAccount(accountId);
    }

    @GetMapping("/all")
    public List<Account> listAll(@AuthenticationPrincipal Account currentAccount) {
        authorization.requireAdmin(currentAccount);
        return accounts.findAll();
    }

    @PostMapping("/{accountId}/deposit")
    public List<WalletBalanceView> deposit(
            @AuthenticationPrincipal Account currentAccount,
            @PathVariable long accountId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody DepositRequest request
    ) {
        authorization.requireOwnerOrAdmin(accountId, currentAccount);
        accounts.requireAccount(accountId);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            wallets.deposit(accountId, request.asset(), request.amount());
        } else {
            wallets.deposit(accountId, request.asset(), request.amount(), idempotencyKey);
        }
        return walletValuations.balances(accountId);
    }

    @GetMapping("/{accountId}/balances")
    public List<WalletBalanceView> balances(@AuthenticationPrincipal Account currentAccount, @PathVariable long accountId) {
        authorization.requireOwnerOrAdmin(accountId, currentAccount);
        accounts.requireAccount(accountId);
        return walletValuations.balances(accountId);
    }
}
