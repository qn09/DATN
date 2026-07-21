package com.example.exchange.deposit.gateway;

import com.example.exchange.deposit.entity.FiatDepositRequest;

public interface DomesticTransferGateway {
    String name();

    GatewaySubmission submit(FiatDepositRequest request);
}
