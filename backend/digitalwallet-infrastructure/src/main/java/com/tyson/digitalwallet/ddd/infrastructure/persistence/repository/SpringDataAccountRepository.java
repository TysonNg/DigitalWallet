package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataAccountRepository extends JpaRepository<AccountEntity, UUID> {
    Optional<AccountEntity> findByCode(String code);

    Optional<AccountEntity> findByWallet_Id(UUID walletId);

    boolean existsByWallet_Id(UUID walletId);

    boolean existsByCode(String code);
}
