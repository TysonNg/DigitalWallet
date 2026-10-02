package com.tyson.digitalwallet.ddd.infrastructure.persistence.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasherImpl implements com.tyson.digitalwallet.ddd.domain.service.security.PasswordHasher {
    private final PasswordEncoder passwordEncoder;

    public PasswordHasherImpl(PasswordEncoder passwordEncoder){
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String rawPassword) {
        if(rawPassword == null || rawPassword.isBlank()) throw new IllegalStateException("Not found password");

        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hashedPassword) {
        if(rawPassword == null || hashedPassword == null) throw new IllegalStateException("Not found rawPassword or hashedPassword");
        boolean matched = passwordEncoder.matches(rawPassword, hashedPassword);

        return matched;
    }
}
