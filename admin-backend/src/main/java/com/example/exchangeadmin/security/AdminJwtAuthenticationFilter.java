package com.example.exchangeadmin.security;

import com.example.exchangeadmin.auth.AdminAccountRepository;
import com.example.exchangeadmin.auth.AdminJwtService;
import com.example.exchangeadmin.auth.AdminPrincipal;
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
public class AdminJwtAuthenticationFilter extends OncePerRequestFilter {
    private final AdminJwtService jwt;
    private final AdminAccountRepository accounts;

    public AdminJwtAuthenticationFilter(AdminJwtService jwt, AdminAccountRepository accounts) {
        this.jwt = jwt;
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            jwt.verify(authorization.substring(7))
                    .filter(claims -> "ADMIN".equals(claims.role()))
                    .flatMap(claims -> accounts.findByUsername(claims.username())
                            .filter(AdminPrincipal::isAdmin)
                            .filter(account -> account.id() == claims.accountId()))
                    .ifPresent(account -> SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    account,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                            )
                    ));
        }
        filterChain.doFilter(request, response);
    }
}
