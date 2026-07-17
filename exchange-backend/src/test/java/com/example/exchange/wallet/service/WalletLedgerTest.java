package com.example.exchange.wallet.service;

import com.example.exchange.ledger.entity.EntryDirection;
import com.example.exchange.ledger.entity.LedgerTransaction;
import com.example.exchange.ledger.repository.InMemoryLedgerRepository;
import com.example.exchange.ledger.service.LedgerService;
import com.example.exchange.trading.entity.MarketSymbol;
import com.example.exchange.trading.entity.Side;
import com.example.exchange.wallet.entity.WalletBalance;
import com.example.exchange.wallet.repository.InMemoryWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletLedgerTest {
    private LedgerService ledger;
    private WalletService wallets;

    @BeforeEach
    void setUp() {
        ledger = new LedgerService(new InMemoryLedgerRepository());
        wallets = new WalletService(new InMemoryWalletRepository(), ledger);
    }

    @Test
    void duplicateDepositDoesNotIncreaseBalanceTwice() {
        wallets.deposit(1L, "USDT", new BigDecimal("1000"), "deposit-request-1");
        wallets.deposit(1L, "USDT", new BigDecimal("1000.00"), "deposit-request-1");

        WalletBalance balance = wallets.balances(1L).get(0);
        assertThat(balance.available()).isEqualByComparingTo("1000");
        assertThat(ledger.history(1L, 50)).hasSize(1);
    }

    @Test
    void reserveMovesFundsFromAvailableToLockedAndStaysBalanced() {
        wallets.deposit(1L, "USDT", new BigDecimal("1000"), "deposit-request-1");
        wallets.reserveForOrder(
                1L,
                MarketSymbol.parse("BTC-USDT"),
                Side.BUY,
                new BigDecimal("100"),
                new BigDecimal("2")
        );

        WalletBalance balance = wallets.balances(1L).get(0);
        assertThat(balance.available()).isEqualByComparingTo("800");
        assertThat(balance.locked()).isEqualByComparingTo("200");

        List<LedgerTransaction> history = ledger.history(1L, 50);
        assertThat(history).hasSize(2);
        assertBalanced(history.get(0), "USDT");
    }

    @Test
    void failedReserveDoesNotCreateLedgerTransaction() {
        wallets.deposit(1L, "USDT", new BigDecimal("100"), "deposit-request-1");

        assertThatThrownBy(() -> wallets.reserveForOrder(
                1L,
                MarketSymbol.parse("BTC-USDT"),
                Side.BUY,
                new BigDecimal("100"),
                new BigDecimal("2")
        )).hasMessage("insufficient available USDT");

        assertThat(ledger.history(1L, 50)).hasSize(1);
    }

    private void assertBalanced(LedgerTransaction transaction, String asset) {
        BigDecimal debits = transaction.entries().stream()
                .filter(entry -> entry.asset().equals(asset) && entry.direction() == EntryDirection.DEBIT)
                .map(entry -> entry.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = transaction.entries().stream()
                .filter(entry -> entry.asset().equals(asset) && entry.direction() == EntryDirection.CREDIT)
                .map(entry -> entry.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debits).isEqualByComparingTo(credits);
    }
}
