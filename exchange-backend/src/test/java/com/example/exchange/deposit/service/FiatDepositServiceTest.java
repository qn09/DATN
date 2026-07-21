package com.example.exchange.deposit.service;

import com.example.exchange.deposit.dto.CreateFiatDepositRequest;
import com.example.exchange.deposit.dto.GatewayDepositCallbackRequest;
import com.example.exchange.deposit.entity.FiatDepositRequest;
import com.example.exchange.deposit.entity.FiatDepositStatus;
import com.example.exchange.deposit.gateway.DomesticTransferGateway;
import com.example.exchange.deposit.gateway.GatewaySubmission;
import com.example.exchange.deposit.repository.InMemoryFiatDepositRepository;
import com.example.exchange.deposit.security.GatewayCallbackSignature;
import com.example.exchange.ledger.repository.InMemoryLedgerRepository;
import com.example.exchange.ledger.service.LedgerService;
import com.example.exchange.wallet.entity.WalletBalance;
import com.example.exchange.wallet.repository.InMemoryWalletRepository;
import com.example.exchange.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FiatDepositServiceTest {
    private static final String SECRET = "test-domestic-gateway-webhook-secret";

    private FiatDepositService deposits;
    private GatewayCallbackSignature signatures;
    private WalletService wallets;
    private LedgerService ledger;

    @BeforeEach
    void setUp() {
        signatures = new GatewayCallbackSignature(SECRET);
        ledger = new LedgerService(new InMemoryLedgerRepository());
        wallets = new WalletService(new InMemoryWalletRepository(), ledger);
        DomesticTransferGateway gateway = new DomesticTransferGateway() {
            @Override
            public String name() {
                return "TEST_GATEWAY";
            }

            @Override
            public GatewaySubmission submit(FiatDepositRequest request) {
                return new GatewaySubmission("GW-" + request.requestId());
            }
        };
        deposits = new FiatDepositService(
                new InMemoryFiatDepositRepository(), gateway, signatures, wallets
        );
    }

    @Test
    void depositMovesFromNewToProcessingAndCreditsOnlyAfterSuccessCallback() {
        FiatDepositRequest created = create("request-1", "1000000");
        assertThat(created.status()).isEqualTo(FiatDepositStatus.NEW);
        assertThat(wallets.balances(1L)).isEmpty();

        FiatDepositRequest processing = deposits.submit(created.requestId());
        assertThat(processing.status()).isEqualTo(FiatDepositStatus.PROCESSING);
        assertThat(wallets.balances(1L)).isEmpty();

        GatewayDepositCallbackRequest callback = successCallback(processing, "1000000");
        FiatDepositRequest success = deposits.processCallback(callback, signatures.sign(callback));

        assertThat(success.status()).isEqualTo(FiatDepositStatus.SUCCESS);
        WalletBalance balance = wallets.balances(1L).get(0);
        assertThat(balance.asset()).isEqualTo("VND");
        assertThat(balance.available()).isEqualByComparingTo("1000000");
        assertThat(ledger.history(1L, 50)).singleElement()
                .satisfies(transaction -> assertThat(transaction.referenceType()).isEqualTo("FIAT_DEPOSIT"));
    }

    @Test
    void repeatedSuccessCallbackDoesNotCreditWalletTwice() {
        FiatDepositRequest processing = deposits.submit(create("request-1", "1000000").requestId());
        GatewayDepositCallbackRequest callback = successCallback(processing, "1000000");
        String signature = signatures.sign(callback);

        deposits.processCallback(callback, signature);
        deposits.processCallback(callback, signature);

        assertThat(wallets.balances(1L).get(0).available()).isEqualByComparingTo("1000000");
        assertThat(ledger.history(1L, 50)).hasSize(1);
    }

    @Test
    void invalidSignatureOrAmountDoesNotCreditWallet() {
        FiatDepositRequest processing = deposits.submit(create("request-1", "1000000").requestId());
        GatewayDepositCallbackRequest callback = successCallback(processing, "999999");

        assertThatThrownBy(() -> deposits.processCallback(callback, "invalid"))
                .hasMessage("invalid gateway callback signature");
        assertThatThrownBy(() -> deposits.processCallback(callback, signatures.sign(callback)))
                .hasMessage("gateway callback amount does not match fiat deposit");
        assertThat(wallets.balances(1L)).isEmpty();
        assertThat(ledger.history(1L, 50)).isEmpty();
    }

    @Test
    void createIsIdempotentAndRejectsDifferentAmountForSameKey() {
        FiatDepositRequest first = create("request-1", "1000000");
        FiatDepositRequest repeated = create("request-1", "1000000.00");

        assertThat(repeated.requestId()).isEqualTo(first.requestId());
        assertThatThrownBy(() -> create("request-1", "2000000"))
                .hasMessage("Idempotency-Key was already used with different fiat deposit data");
    }

    @Test
    void failedCallbackIsTerminalAndDoesNotCreditWallet() {
        FiatDepositRequest processing = deposits.submit(create("request-1", "1000000").requestId());
        GatewayDepositCallbackRequest callback = new GatewayDepositCallbackRequest(
                processing.requestId(), processing.gatewayReference(), "FAILED",
                processing.currency(), processing.amount(), "bank rejected transfer"
        );

        FiatDepositRequest failed = deposits.processCallback(callback, signatures.sign(callback));

        assertThat(failed.status()).isEqualTo(FiatDepositStatus.FAILED);
        assertThat(failed.failureReason()).isEqualTo("bank rejected transfer");
        assertThat(wallets.balances(1L)).isEmpty();
    }

    private FiatDepositRequest create(String key, String amount) {
        return deposits.create(
                new CreateFiatDepositRequest(1L, "VND", new BigDecimal(amount)),
                key
        );
    }

    private GatewayDepositCallbackRequest successCallback(FiatDepositRequest request, String amount) {
        return new GatewayDepositCallbackRequest(
                request.requestId(), request.gatewayReference(), "SUCCESS",
                request.currency(), new BigDecimal(amount), null
        );
    }
}
