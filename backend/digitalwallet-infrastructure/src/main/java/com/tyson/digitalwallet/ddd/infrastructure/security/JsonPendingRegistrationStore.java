package com.tyson.digitalwallet.ddd.infrastructure.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.application.usecase.auth.command.PendingRegistration;
import com.tyson.digitalwallet.ddd.application.usecase.auth.port.PendingRegistrationStore;
import com.tyson.digitalwallet.ddd.infrastructure.cached.RedisInfrasService;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class JsonPendingRegistrationStore implements PendingRegistrationStore {
    private static final String PENDING_PREFIX = "pending:register:";

    private final RedisInfrasService redis;
    private final ObjectMapper objectMapper;

    public JsonPendingRegistrationStore(RedisInfrasService redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(PendingRegistration registration, long expirationMinutes) {
        try {
            String json = objectMapper.writeValueAsString(registration);
            redis.set(PENDING_PREFIX + registration.email(), json, expirationMinutes, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to store pending registration data", e);
        }
    }

    @Override
    public Optional<PendingRegistration> findByEmail(String email) {
        String json = redis.get(PENDING_PREFIX + email);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(objectMapper.readValue(json, PendingRegistration.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to process registration data", e);
        }
    }

    @Override
    public void delete(String email) {
        redis.delete(PENDING_PREFIX + email);
    }
}