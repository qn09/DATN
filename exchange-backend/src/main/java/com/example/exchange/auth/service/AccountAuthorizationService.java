package com.example.exchange.auth.service;

import com.example.exchange.auth.entity.Account;
import com.example.exchange.auth.entity.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AccountAuthorizationService {
    public void requireOwnerOrAdmin(long accountId, Account currentAccount) {
        requireAuthenticated(currentAccount);
        if (currentAccount.role() == Role.ADMIN || currentAccount.id() == accountId) {
            return;
        }
        throw new AccessDeniedException("account access denied");
    }

    public void requireAdmin(Account currentAccount) {
        requireAuthenticated(currentAccount);
        if (currentAccount.role() != Role.ADMIN) {
            throw new AccessDeniedException("admin role required");
        }
    }

    private void requireAuthenticated(Account currentAccount) {
        if (currentAccount == null) {
            throw new AccessDeniedException("authentication required");
        }
    }
}
