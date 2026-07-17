package com.example.exchange.config;

import com.example.exchange.auth.entity.Role;
import com.example.exchange.auth.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrapConfig {
    @Bean
    public CommandLineRunner adminBootstrap(
            AccountRepository accounts,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username:admin}") String username,
            @Value("${app.admin.password:admin123}") String password
    ) {
        return args -> {
            String passwordHash = passwordEncoder.encode(password);
            accounts.findByUsername(username)
                    .map(account -> accounts.updateCredentialsAndRole(username, passwordHash, Role.ADMIN))
                    .orElseGet(() -> accounts.create(username, passwordHash, Role.ADMIN));
        };
    }
}
