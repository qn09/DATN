package com.example.exchange.trading.service;

import com.example.exchange.auth.service.AccountService;
import com.example.exchange.trading.dto.PlaceOrderRequest;
import com.example.exchange.auth.entity.Account;
import com.example.exchange.wallet.entity.WalletBalance;
import com.example.exchange.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingEngineTest {
    private AccountService accountService;
    private WalletService walletService;
    private MatchingEngine matchingEngine;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService();
        walletService = new WalletService();
        matchingEngine = new MatchingEngine(walletService);
        orderService = new OrderService(accountService, walletService, matchingEngine);
    }

    @Test
    void buyAndSellOrdersMatchAndSettleBalances() {
        Account buyer = accountService.create("alice");
        Account seller = accountService.create("bob");
        walletService.deposit(buyer.id(), "USDT", new BigDecimal("100000"));
        walletService.deposit(seller.id(), "BTC", new BigDecimal("2"));

        orderService.place(new PlaceOrderRequest(
                buyer.id(),
                "BTC-USDT",
                "BUY",
                new BigDecimal("30000"),
                new BigDecimal("1")
        ));

        var result = orderService.place(new PlaceOrderRequest(
                seller.id(),
                "BTC-USDT",
                "SELL",
                new BigDecimal("29000"),
                new BigDecimal("1")
        ));

        assertThat(result.trades()).hasSize(1);
        assertThat(result.trades().get(0).price()).isEqualByComparingTo("30000");

        Map<String, WalletBalance> buyerBalances = balancesByAsset(buyer.id());
        assertThat(buyerBalances.get("BTC").available()).isEqualByComparingTo("1");
        assertThat(buyerBalances.get("USDT").available()).isEqualByComparingTo("70000");
        assertThat(buyerBalances.get("USDT").locked()).isEqualByComparingTo("0");

        Map<String, WalletBalance> sellerBalances = balancesByAsset(seller.id());
        assertThat(sellerBalances.get("BTC").available()).isEqualByComparingTo("1");
        assertThat(sellerBalances.get("BTC").locked()).isEqualByComparingTo("0");
        assertThat(sellerBalances.get("USDT").available()).isEqualByComparingTo("30000");
    }

    @Test
    void openBuyOrderLocksQuoteAsset() {
        Account buyer = accountService.create("alice");
        walletService.deposit(buyer.id(), "USDT", new BigDecimal("100000"));

        orderService.place(new PlaceOrderRequest(
                buyer.id(),
                "BTC-USDT",
                "BUY",
                new BigDecimal("30000"),
                new BigDecimal("2")
        ));

        Map<String, WalletBalance> balances = balancesByAsset(buyer.id());
        assertThat(balances.get("USDT").available()).isEqualByComparingTo("40000");
        assertThat(balances.get("USDT").locked()).isEqualByComparingTo("60000");
    }

    @Test
    void incomingBuyReceivesPriceImprovementRefund() {
        Account buyer = accountService.create("alice");
        Account seller = accountService.create("bob");
        walletService.deposit(buyer.id(), "USDT", new BigDecimal("30000"));
        walletService.deposit(seller.id(), "BTC", BigDecimal.ONE);

        orderService.place(new PlaceOrderRequest(
                seller.id(), "BTC-USDT", "SELL", new BigDecimal("29000"), BigDecimal.ONE
        ));
        var result = orderService.place(new PlaceOrderRequest(
                buyer.id(), "BTC-USDT", "BUY", new BigDecimal("30000"), BigDecimal.ONE
        ));

        assertThat(result.trades()).hasSize(1);
        assertThat(result.trades().get(0).price()).isEqualByComparingTo("29000");

        Map<String, WalletBalance> buyerBalances = balancesByAsset(buyer.id());
        assertThat(buyerBalances.get("BTC").available()).isEqualByComparingTo("1");
        assertThat(buyerBalances.get("USDT").available()).isEqualByComparingTo("1000");
        assertThat(buyerBalances.get("USDT").locked()).isEqualByComparingTo("0");

        Map<String, WalletBalance> sellerBalances = balancesByAsset(seller.id());
        assertThat(sellerBalances.get("USDT").available()).isEqualByComparingTo("29000");
    }

    private Map<String, WalletBalance> balancesByAsset(long accountId) {
        return walletService.balances(accountId).stream()
                .collect(Collectors.toMap(WalletBalance::asset, balance -> balance));
    }
}
