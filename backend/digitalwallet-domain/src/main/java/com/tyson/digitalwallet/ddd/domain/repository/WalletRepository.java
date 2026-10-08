package com.tyson.digitalwallet.ddd.domain.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {
    boolean existsByUserId(UUID userId);
    Optional<Wallet> findById(UUID id);
    Optional<Wallet> findByIdWithLock(UUID id);
    Optional<Wallet> findByUserId(UUID userId);
    Wallet saveWallet(Wallet wallet);
}
