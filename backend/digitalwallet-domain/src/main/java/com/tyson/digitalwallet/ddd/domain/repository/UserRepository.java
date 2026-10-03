package com.tyson.digitalwallet.ddd.domain.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    boolean existByEmail(String email);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    User save(User user);
}
