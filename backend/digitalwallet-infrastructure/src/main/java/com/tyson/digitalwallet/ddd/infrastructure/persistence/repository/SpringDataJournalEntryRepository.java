package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.JournalEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataJournalEntryRepository extends JpaRepository<JournalEntryEntity, UUID> {
    Optional<JournalEntryEntity> findByTransactionId(UUID transactionId);

    Optional<JournalEntryEntity> findByPaymentOrderId(UUID paymentOrderId);
}
