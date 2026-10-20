package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.LedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface SpringDataLedgerEntryRepository extends JpaRepository<LedgerEntryEntity, UUID> {
    List<LedgerEntryEntity> findByAccount_IdOrderByCreatedAtDesc(UUID accountId);

    List<LedgerEntryEntity> findByJournalEntry_Id(UUID journalEntryId);

    @Query("SELECT COALESCE(SUM(l.debitAmount), 0) FROM LedgerEntryEntity l")
    BigDecimal sumTotalDebit();

    @Query("SELECT COALESCE(SUM(l.creditAmount), 0) FROM LedgerEntryEntity l")
    BigDecimal sumTotalCredit();

    @Query("SELECT COALESCE(SUM(l.creditAmount), 0) - COALESCE(SUM(l.debitAmount), 0) FROM LedgerEntryEntity l WHERE l.account.id = :accountId")
    BigDecimal calculateBalanceForLiabilityAccount(@Param("accountId") UUID accountId);
}
