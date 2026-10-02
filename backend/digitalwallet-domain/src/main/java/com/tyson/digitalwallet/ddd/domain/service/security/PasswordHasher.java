package com.tyson.digitalwallet.ddd.domain.service.security;

public interface PasswordHasher {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String hashedPassword);
}
