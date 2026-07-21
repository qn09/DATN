package com.example.exchange.deposit.controller;

import com.example.exchange.auth.entity.Account;
import com.example.exchange.auth.service.AccountAuthorizationService;
import com.example.exchange.auth.service.AccountService;
import com.example.exchange.deposit.dto.CreateFiatDepositRequest;
import com.example.exchange.deposit.dto.FiatDepositResponse;
import com.example.exchange.deposit.dto.GatewayDepositCallbackRequest;
import com.example.exchange.deposit.entity.FiatDepositRequest;
import com.example.exchange.deposit.service.FiatDepositService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fiat-deposits")
public class FiatDepositController {
    private final FiatDepositService deposits;
    private final AccountService accounts;
    private final AccountAuthorizationService authorization;

    public FiatDepositController(
            FiatDepositService deposits,
            AccountService accounts,
            AccountAuthorizationService authorization
    ) {
        this.deposits = deposits;
        this.accounts = accounts;
        this.authorization = authorization;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FiatDepositResponse create(
            @AuthenticationPrincipal Account currentAccount,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreateFiatDepositRequest request
    ) {
        authorization.requireOwnerOrAdmin(request.accountId(), currentAccount);
        accounts.requireAccount(request.accountId());
        return FiatDepositResponse.from(deposits.create(request, idempotencyKey));
    }

    @PostMapping("/{requestId}/submit")
    public FiatDepositResponse submit(
            @AuthenticationPrincipal Account currentAccount,
            @PathVariable String requestId
    ) {
        FiatDepositRequest request = deposits.require(requestId);
        authorization.requireOwnerOrAdmin(request.accountId(), currentAccount);
        return FiatDepositResponse.from(deposits.submit(requestId));
    }

    @GetMapping("/{requestId}")
    public FiatDepositResponse get(
            @AuthenticationPrincipal Account currentAccount,
            @PathVariable String requestId
    ) {
        FiatDepositRequest request = deposits.require(requestId);
        authorization.requireOwnerOrAdmin(request.accountId(), currentAccount);
        return FiatDepositResponse.from(request);
    }

    @GetMapping("/accounts/{accountId}")
    public List<FiatDepositResponse> history(
            @AuthenticationPrincipal Account currentAccount,
            @PathVariable long accountId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        authorization.requireOwnerOrAdmin(accountId, currentAccount);
        accounts.requireAccount(accountId);
        return deposits.history(accountId, limit).stream().map(FiatDepositResponse::from).toList();
    }

    @PostMapping("/gateway/callback")
    public FiatDepositResponse callback(
            @RequestHeader(value = "X-Gateway-Signature", required = false) String signature,
            @RequestBody GatewayDepositCallbackRequest callback
    ) {
        return FiatDepositResponse.from(deposits.processCallback(callback, signature));
    }
}
