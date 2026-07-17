package com.example.exchange.config;

import com.example.exchange.auth.repository.AccountRepository;
import com.example.exchange.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final AccountRepository accounts;

    public JwtAuthenticationFilter(JwtService jwt, AccountRepository accounts) {
        this.jwt = jwt;
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            jwt.verify(header.substring(7)).flatMap(claims -> accounts.findByUsername(claims.username()))
                    .ifPresent(account -> {
                        var authentication = new UsernamePasswordAuthenticationToken(
                                account,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + account.role().name()))
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }

        filterChain.doFilter(request, response);
    }
}
