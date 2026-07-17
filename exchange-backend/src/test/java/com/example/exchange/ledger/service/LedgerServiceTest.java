package com.example.exchange.ledger.service;

import com.example.exchange.ledger.entity.EntryDirection;
import com.example.exchange.ledger.entity.LedgerAccountType;
import com.example.exchange.ledger.entity.LedgerPosting;
import com.example.exchange.ledger.repository.InMemoryLedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerServiceTest {
    private LedgerService ledger;

    @BeforeEach
    void setUp() {
        ledger = new LedgerService(new InMemoryLedgerRepository());
    }

    @Test
    void balancedTransactionIsPosted() {
        LedgerPostResult result = ledger.post(
                "DEPOSIT",
                "deposit-1",
                "deposit:1:deposit-1",
                "Deposit USDT",
                depositPostings("100")
        );

        assertThat(result.created()).isTrue();
        assertThat(result.transaction().entries()).hasSize(2);
        assertThat(ledger.history(1L, 50)).hasSize(1);
    }

    @Test
    void unbalancedTransactionIsRejected() {
        List<LedgerPosting> postings = List.of(
                LedgerPosting.system(
                        "USDT", LedgerAccountType.SYSTEM_EXTERNAL,
                        EntryDirection.DEBIT, new BigDecimal("100")
                ),
                LedgerPosting.user(
                        1L, "USDT", LedgerAccountType.USER_AVAILABLE,
                        EntryDirection.CREDIT, new BigDecimal("99")
                )
        );

        assertThatThrownBy(() -> ledger.post(
                "DEPOSIT", "deposit-1", "deposit:1:deposit-1", "Deposit USDT", postings
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unbalanced ledger transaction for USDT");
        assertThat(ledger.history(1L, 50)).isEmpty();
    }

    @Test
    void sameIdempotencyKeyReturnsExistingTransaction() {
        LedgerPostResult first = ledger.post(
                "DEPOSIT", "deposit-1", "deposit:1:deposit-1", "Deposit USDT", depositPostings("100")
        );
        LedgerPostResult second = ledger.post(
                "DEPOSIT", "deposit-1", "deposit:1:deposit-1", "Deposit USDT", depositPostings("100.00")
        );

        assertThat(first.created()).isTrue();
        assertThat(second.created()).isFalse();
        assertThat(second.transaction().id()).isEqualTo(first.transaction().id());
        assertThat(ledger.history(1L, 50)).hasSize(1);
    }

    @Test
    void reusedIdempotencyKeyWithDifferentAmountIsRejected() {
        ledger.post(
                "DEPOSIT", "deposit-1", "deposit:1:deposit-1", "Deposit USDT", depositPostings("100")
        );

        assertThatThrownBy(() -> ledger.post(
                "DEPOSIT", "deposit-1", "deposit:1:deposit-1", "Deposit USDT", depositPostings("200")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("idempotency key was already used with different ledger data");
    }

    private List<LedgerPosting> depositPostings(String amount) {
        BigDecimal value = new BigDecimal(amount);
        return List.of(
                LedgerPosting.system(
                        "USDT", LedgerAccountType.SYSTEM_EXTERNAL, EntryDirection.DEBIT, value
                ),
                LedgerPosting.user(
                        1L, "USDT", LedgerAccountType.USER_AVAILABLE, EntryDirection.CREDIT, value
                )
        );
    }
}
