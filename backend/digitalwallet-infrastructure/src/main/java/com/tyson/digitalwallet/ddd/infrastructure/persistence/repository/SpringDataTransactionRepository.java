package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataTransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    @Query("SELECT t FROM TransactionEntity t WHERE t.senderWallet.id = :walletId OR t.receiverWallet.id = :walletId ORDER BY t.createdAt DESC")
    List<TransactionEntity> findByWalletId(@Param("walletId") UUID walletId);
}
