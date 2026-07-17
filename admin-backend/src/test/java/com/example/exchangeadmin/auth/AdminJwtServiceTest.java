package com.example.exchangeadmin.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminJwtServiceTest {
    private final AdminJwtService jwt = new AdminJwtService(
            new ObjectMapper(),
            "a-test-secret-that-is-long-enough-for-local-tests",
            3600
    );

    @Test
    void createsAndVerifiesAdminClaims() {
        String token = jwt.createToken(new AdminPrincipal(42, "admin", "ignored", "ADMIN"));

        AdminJwtService.AdminJwtClaims claims = jwt.verify(token).orElseThrow();

        assertThat(claims.username()).isEqualTo("admin");
        assertThat(claims.accountId()).isEqualTo(42);
        assertThat(claims.role()).isEqualTo("ADMIN");
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwt.createToken(new AdminPrincipal(42, "admin", "ignored", "ADMIN"));

        assertThat(jwt.verify(token + "changed")).isEmpty();
    }
}
