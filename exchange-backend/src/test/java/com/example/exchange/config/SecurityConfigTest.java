package com.example.exchange.config;

import com.example.exchange.auth.controller.AuthController;
import com.example.exchange.auth.dto.AuthResponse;
import com.example.exchange.auth.entity.Account;
import com.example.exchange.auth.entity.Role;
import com.example.exchange.auth.repository.AccountRepository;
import com.example.exchange.auth.service.AuthService;
import com.example.exchange.auth.service.JwtService;
import com.example.exchange.auth.service.LoginRateLimiterService;
import com.example.exchange.market.controller.MarketController;
import com.example.exchange.market.service.BinanceDepthStreamService;
import com.example.exchange.market.service.BinanceMarketDataService;
import com.example.exchange.market.service.MarketPriceService;
import io.github.bucket4j.ConsumptionProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {AuthController.class, MarketController.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = GlobalRateLimitFilter.class
        )
)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SecurityConfigTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private LoginRateLimiterService loginRateLimiter;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private MarketPriceService marketPriceService;

    @MockBean
    private BinanceMarketDataService binanceMarketDataService;

    @MockBean
    private BinanceDepthStreamService binanceDepthStreamService;

    private final Account account = new Account(1L, "buyer", "hash", Role.USER);

    @BeforeEach
    void setUp() {
        when(loginRateLimiter.tryConsume(any(), any()))
                .thenReturn(ConsumptionProbe.consumed(4, 0));
        when(authService.login(any())).thenReturn(new AuthResponse("valid-token", account));
    }

    @Test
    void loginDoesNotRequireJwt() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"buyer\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("valid-token"));
    }

    @Test
    void everyOtherEndpointRequiresJwt() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"buyer\",\"password\":\"secret123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"authentication required\"}"));

        mockMvc.perform(get("/api/market/prices"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"authentication required\"}"));
    }

    @Test
    void protectedEndpointAcceptsValidJwt() throws Exception {
        when(jwtService.verify("valid-token"))
                .thenReturn(Optional.of(new JwtService.JwtClaims("buyer", 1L, Role.USER)));
        when(accountRepository.findByUsername("buyer")).thenReturn(Optional.of(account));
        when(marketPriceService.getPopularPrices()).thenReturn(List.of());

        mockMvc.perform(get("/api/market/prices")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
