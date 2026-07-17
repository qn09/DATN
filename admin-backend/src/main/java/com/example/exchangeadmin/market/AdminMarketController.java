package com.example.exchangeadmin.market;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/market")
public class AdminMarketController {
    private final AdminMarketPriceService prices;

    public AdminMarketController(AdminMarketPriceService prices) {
        this.prices = prices;
    }

    @GetMapping("/prices")
    public List<AdminMarketPriceResponse> prices() {
        return prices.getPopularPrices();
    }
}
