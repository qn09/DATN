package com.example.exchange.deposit.gateway;

import com.example.exchange.deposit.entity.FiatDepositRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MockDomesticTransferGateway implements DomesticTransferGateway {
    private final String gatewayName;

    public MockDomesticTransferGateway(
            @Value("${app.fiat-deposit.gateway:MOCK_DOMESTIC}") String gatewayName
    ) {
        this.gatewayName = gatewayName;
    }

    @Override
    public String name() {
        return gatewayName;
    }

    @Override
    public GatewaySubmission submit(FiatDepositRequest request) {
        return new GatewaySubmission("MOCK-" + request.requestId());
    }
}
