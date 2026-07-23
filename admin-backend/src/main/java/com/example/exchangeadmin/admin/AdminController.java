package com.example.exchangeadmin.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminQueryService queries;
    private final AdminFiatDepositCommandService depositCommands;

    public AdminController(
            AdminQueryService queries,
            AdminFiatDepositCommandService depositCommands
    ) {
        this.queries = queries;
        this.depositCommands = depositCommands;
    }

    @GetMapping("/summary")
    public AdminSummaryResponse summary() {
        return queries.summary();
    }

    @GetMapping("/accounts")
    public List<AdminAccountView> accounts() {
        return queries.accounts();
    }

    @GetMapping("/orders")
    public List<AdminOrderView> orders() {
        return queries.orders();
    }

    @GetMapping("/trades")
    public List<AdminTradeView> trades(@RequestParam(defaultValue = "50") int limit) {
        return queries.trades(limit);
    }

    @GetMapping("/fiat-deposits")
    public List<AdminFiatDepositView> fiatDeposits(@RequestParam(defaultValue = "50") int limit) {
        return queries.fiatDeposits(limit);
    }

    @PostMapping("/fiat-deposits/{requestId}/decision")
    public AdminFiatDepositView decideFiatDeposit(
            @PathVariable String requestId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody AdminFiatDepositDecisionRequest request
    ) {
        return depositCommands.decide(requestId, idempotencyKey, request);
    }
}
