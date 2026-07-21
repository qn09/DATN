package com.example.exchange.ledger.service;

import com.example.exchange.common.AssetCatalog;
import com.example.exchange.ledger.entity.EntryDirection;
import com.example.exchange.ledger.entity.LedgerAccountType;
import com.example.exchange.ledger.entity.LedgerPosting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class LedgerOpeningBalanceInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LedgerOpeningBalanceInitializer.class);

    private final JdbcTemplate jdbc;
    private final LedgerService ledger;

    public LedgerOpeningBalanceInitializer(JdbcTemplate jdbc, LedgerService ledger) {
        this.jdbc = jdbc;
        this.ledger = ledger;
    }

    @Override
    public void run(ApplicationArguments args) {
        findBalancesWithoutLedger().forEach(balance -> {
            if (!AssetCatalog.SUPPORTED_ASSETS.contains(balance.asset())) {
                log.warn(
                        "Skipping opening ledger balance for account {} with unsupported asset {}",
                        balance.accountId(),
                        balance.asset()
                );
                return;
            }
            recordOpeningBalance(balance);
        });
    }

    private List<OpeningBalance> findBalancesWithoutLedger() {
        return jdbc.query(
                """
                        SELECT balance.account_id, balance.asset, balance.available, balance.locked
                        FROM wallet_balances balance
                        WHERE (balance.available > 0 OR balance.locked > 0)
                          AND NOT EXISTS (
                              SELECT 1
                              FROM ledger_entries entry
                              JOIN ledger_accounts account ON account.id = entry.ledger_account_id
                              WHERE account.owner_account_id = balance.account_id
                                AND account.asset = balance.asset
                          )
                        ORDER BY balance.account_id, balance.asset
                        """,
                (rs, rowNum) -> new OpeningBalance(
                        rs.getLong("account_id"),
                        rs.getString("asset"),
                        rs.getBigDecimal("available").stripTrailingZeros(),
                        rs.getBigDecimal("locked").stripTrailingZeros()
                )
        );
    }

    private void recordOpeningBalance(OpeningBalance balance) {
        BigDecimal total = balance.available().add(balance.locked());
        List<LedgerPosting> postings = new ArrayList<>();
        postings.add(LedgerPosting.system(
                balance.asset(),
                LedgerAccountType.SYSTEM_EXTERNAL,
                EntryDirection.DEBIT,
                total
        ));
        if (balance.available().signum() > 0) {
            postings.add(LedgerPosting.user(
                    balance.accountId(),
                    balance.asset(),
                    LedgerAccountType.USER_AVAILABLE,
                    EntryDirection.CREDIT,
                    balance.available()
            ));
        }
        if (balance.locked().signum() > 0) {
            postings.add(LedgerPosting.user(
                    balance.accountId(),
                    balance.asset(),
                    LedgerAccountType.USER_LOCKED,
                    EntryDirection.CREDIT,
                    balance.locked()
            ));
        }

        String referenceId = balance.accountId() + ":" + balance.asset();
        ledger.post(
                "OPENING_BALANCE",
                referenceId,
                "opening-balance:" + referenceId,
                "Opening balance imported from wallet_balances",
                postings
        );
    }

    private record OpeningBalance(
            long accountId,
            String asset,
            BigDecimal available,
            BigDecimal locked
    ) {
    }
}
