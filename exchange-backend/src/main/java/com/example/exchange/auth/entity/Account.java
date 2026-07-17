package com.example.exchange.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record Account(long id, String username, @JsonIgnore String passwordHash, Role role) {
    public Account(long id, String username) {
        this(id, username, null, Role.USER);
    }
}
