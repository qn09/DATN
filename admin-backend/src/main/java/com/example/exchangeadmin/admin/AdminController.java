package com.example.exchangeadmin.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminQueryService queries;

    public AdminController(AdminQueryService queries) {
        this.queries = queries;
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
}
