package com.tyson.digitalwallet.ddd.domain.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.Account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Optional<Account> findById(UUID id);

    Optional<Account> findByCode(String code);

    Optional<Account> findByWalletId(UUID walletId);

    boolean existsByWalletId(UUID walletId);

    Account save(Account account);

    List<Account> findAll();
}
