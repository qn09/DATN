package com.example.exchange.config;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalRateLimitFilter extends OncePerRequestFilter {
    private final GlobalRateLimiterService globalRateLimiter;
    private final IpRateLimiterService ipRateLimiter;

    public GlobalRateLimitFilter(GlobalRateLimiterService globalRateLimiter, IpRateLimiterService ipRateLimiter) {
        this.globalRateLimiter = globalRateLimiter;
        this.ipRateLimiter = ipRateLimiter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!shouldRateLimit(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        ConsumptionProbe probe = globalRateLimiter.tryConsume();
        response.setHeader("X-Rate-Limit-Limit", String.valueOf(globalRateLimiter.capacity()));
        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));

        if (!probe.isConsumed()) {
            long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(Math.max(1, retryAfterSeconds)));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(String.format(
                    "{\"error\":\"global rate limit exceeded\",\"limit\":\"%d requests per %d seconds\"}",
                    globalRateLimiter.capacity(),
                    globalRateLimiter.windowSeconds()
            ));
            return;
        }

        ConsumptionProbe ipProbe = ipRateLimiter.tryConsume(clientIp(request));
        response.setHeader("X-Rate-Limit-IP-Limit", String.valueOf(ipRateLimiter.capacity()));
        response.setHeader("X-Rate-Limit-IP-Remaining", String.valueOf(ipProbe.getRemainingTokens()));

        if (!ipProbe.isConsumed()) {
            long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(ipProbe.getNanosToWaitForRefill());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(Math.max(1, retryAfterSeconds)));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(String.format(
                    "{\"error\":\"ip rate limit exceeded\",\"limit\":\"%d requests per %d seconds\"}",
                    ipRateLimiter.capacity(),
                    ipRateLimiter.windowSeconds()
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldRateLimit(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/")
                && !HttpMethod.OPTIONS.matches(request.getMethod());
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
