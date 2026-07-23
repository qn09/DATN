package com.example.exchangeadmin.admin;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AdminFiatDepositCommandServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-23T07:00:00Z");

    @Test
    void approveSendsSignedIdempotentCallbackToExchangeBackend() {
        AdminQueryService queries = mock(AdminQueryService.class);
        AdminFiatDepositView processing = deposit("PROCESSING", null);
        AdminFiatDepositView success = deposit("SUCCESS", null);
        when(queries.requireFiatDeposit("deposit-1")).thenReturn(processing, success);

        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AdminFiatDepositCommandService service = new AdminFiatDepositCommandService(
                queries,
                new AdminGatewayCallbackSignature("test-secret"),
                builder.build(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        server.expect(requestTo("http://localhost/api/fiat-deposits/gateway/callback"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Gateway-Signature", org.hamcrest.Matchers.not(org.hamcrest.Matchers.blankString())))
                .andExpect(jsonPath("$.eventId").value("event-1"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andRespond(withSuccess());

        AdminFiatDepositView result = service.decide(
                "deposit-1",
                "event-1",
                new AdminFiatDepositDecisionRequest("SUCCESS", null)
        );

        assertThat(result.status()).isEqualTo("SUCCESS");
        server.verify();
    }

    @Test
    void rejectRequiresReason() {
        AdminQueryService queries = mock(AdminQueryService.class);
        when(queries.requireFiatDeposit("deposit-1")).thenReturn(deposit("PROCESSING", null));
        AdminFiatDepositCommandService service = new AdminFiatDepositCommandService(
                queries,
                new AdminGatewayCallbackSignature("test-secret"),
                RestClient.create("http://localhost"),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> service.decide(
                "deposit-1", "event-1", new AdminFiatDepositDecisionRequest("FAILED", " ")
        )).hasMessage("failureReason is required when rejecting a deposit");
    }

    @Test
    void repeatedTerminalDecisionDoesNotCallExchangeBackend() {
        AdminQueryService queries = mock(AdminQueryService.class);
        AdminFiatDepositView success = deposit("SUCCESS", null);
        when(queries.requireFiatDeposit("deposit-1")).thenReturn(success);
        AdminFiatDepositCommandService service = new AdminFiatDepositCommandService(
                queries,
                new AdminGatewayCallbackSignature("test-secret"),
                RestClient.create("http://localhost"),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        assertThat(service.decide(
                "deposit-1", "event-1", new AdminFiatDepositDecisionRequest("SUCCESS", null)
        )).isEqualTo(success);
        verify(queries).requireFiatDeposit("deposit-1");
        verifyNoMoreInteractions(queries);
    }

    private AdminFiatDepositView deposit(String status, String failureReason) {
        return new AdminFiatDepositView(
                "deposit-1",
                1L,
                "buyer",
                "VND",
                new BigDecimal("1000000"),
                status,
                "MOCK_DOMESTIC",
                "GW-deposit-1",
                failureReason,
                NOW.minusSeconds(60),
                NOW.minusSeconds(30),
                "PROCESSING".equals(status) ? null : NOW
        );
    }
}
