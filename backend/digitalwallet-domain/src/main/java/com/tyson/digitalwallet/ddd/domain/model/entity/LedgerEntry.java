package com.tyson.digitalwallet.ddd.domain.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class LedgerEntry {
    private UUID id;
    private UUID journalEntryId;
    private UUID accountId;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private LocalDateTime createdAt;

    public LedgerEntry() {
    }

    public LedgerEntry(UUID id, UUID journalEntryId, UUID accountId, BigDecimal debitAmount, BigDecimal creditAmount, LocalDateTime createdAt) {
        BigDecimal debit = (debitAmount != null) ? debitAmount : BigDecimal.ZERO;
        BigDecimal credit = (creditAmount != null) ? creditAmount : BigDecimal.ZERO;

        if (debit.compareTo(BigDecimal.ZERO) < 0 || credit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số tiền Nợ (Debit) và Có (Credit) trong sổ cái không được âm");
        }

        boolean isDebitPositive = debit.compareTo(BigDecimal.ZERO) > 0;
        boolean isCreditPositive = credit.compareTo(BigDecimal.ZERO) > 0;

        if (isDebitPositive && isCreditPositive) {
            throw new IllegalArgumentException("Một dòng bút toán sổ cái không thể đồng thời vừa ghi Nợ vừa ghi Có");
        }
        if (!isDebitPositive && !isCreditPositive) {
            throw new IllegalArgumentException("Một dòng bút toán sổ cái phải có số tiền Nợ hoặc Có lớn hơn 0");
        }

        this.id = (id != null) ? id : UUID.randomUUID();
        this.journalEntryId = journalEntryId;
        this.accountId = accountId;
        this.debitAmount = debit;
        this.creditAmount = credit;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    public static LedgerEntry debit(UUID journalEntryId, UUID accountId, BigDecimal amount) {
        return new LedgerEntry(UUID.randomUUID(), journalEntryId, accountId, amount, BigDecimal.ZERO, LocalDateTime.now());
    }

    public static LedgerEntry credit(UUID journalEntryId, UUID accountId, BigDecimal amount) {
        return new LedgerEntry(UUID.randomUUID(), journalEntryId, accountId, BigDecimal.ZERO, amount, LocalDateTime.now());
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(UUID journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
