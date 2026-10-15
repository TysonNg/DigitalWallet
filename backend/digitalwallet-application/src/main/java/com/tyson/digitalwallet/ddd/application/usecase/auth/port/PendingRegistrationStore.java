package com.tyson.digitalwallet.ddd.application.usecase.auth.port;

import com.tyson.digitalwallet.ddd.application.usecase.auth.command.PendingRegistration;
import java.util.Optional;

public interface PendingRegistrationStore {
    void save(PendingRegistration registration, long expirationMinutes);
    Optional<PendingRegistration> findByEmail(String email);
    void delete(String email);
}