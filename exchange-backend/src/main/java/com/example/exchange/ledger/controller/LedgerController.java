package com.example.exchange.ledger.controller;

import com.example.exchange.auth.entity.Account;
import com.example.exchange.auth.entity.Role;
import com.example.exchange.auth.service.AccountAuthorizationService;
import com.example.exchange.auth.service.AccountService;
import com.example.exchange.ledger.dto.LedgerEntryResponse;
import com.example.exchange.ledger.dto.LedgerTransactionResponse;
import com.example.exchange.ledger.entity.LedgerEntry;
import com.example.exchange.ledger.entity.LedgerTransaction;
import com.example.exchange.ledger.service.LedgerService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts/{accountId}/ledger")
public class LedgerController {
    private final LedgerService ledger;
    private final AccountService accounts;
    private final AccountAuthorizationService authorization;

    public LedgerController(
            LedgerService ledger,
            AccountService accounts,
            AccountAuthorizationService authorization
    ) {
        this.ledger = ledger;
        this.accounts = accounts;
        this.authorization = authorization;
    }

    @GetMapping
    public List<LedgerTransactionResponse> history(
            @AuthenticationPrincipal Account currentAccount,
            @PathVariable long accountId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        authorization.requireOwnerOrAdmin(accountId, currentAccount);
        accounts.requireAccount(accountId);
        return ledger.history(accountId, limit).stream()
                .map(transaction -> toResponse(transaction, accountId, currentAccount.role() == Role.ADMIN))
                .toList();
    }

    private LedgerTransactionResponse toResponse(
            LedgerTransaction transaction,
            long requestedAccountId,
            boolean admin
    ) {
        return new LedgerTransactionResponse(
                transaction.id(),
                transaction.referenceType(),
                transaction.referenceId(),
                transaction.description(),
                transaction.createdAt(),
                transaction.entries().stream()
                        .map(entry -> toResponse(entry, requestedAccountId, admin))
                        .toList()
        );
    }

    private LedgerEntryResponse toResponse(LedgerEntry entry, long requestedAccountId, boolean admin) {
        Long visibleOwnerAccountId = entry.ownerAccountId();
        if (!admin && visibleOwnerAccountId != null && visibleOwnerAccountId != requestedAccountId) {
            visibleOwnerAccountId = null;
        }
        return new LedgerEntryResponse(
                visibleOwnerAccountId,
                entry.asset(),
                entry.accountType(),
                entry.direction(),
                entry.amount()
        );
    }
}
