package com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntryEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntryEntity journalEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(name = "debit_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal debitAmount;

    @Column(name = "credit_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal creditAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public LedgerEntryEntity() {
    }

    public LedgerEntryEntity(UUID id, JournalEntryEntity journalEntry, AccountEntity account, BigDecimal debitAmount, BigDecimal creditAmount, LocalDateTime createdAt) {
        this.id = id;
        this.journalEntry = journalEntry;
        this.account = account;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public JournalEntryEntity getJournalEntry() {
        return journalEntry;
    }

    public void setJournalEntry(JournalEntryEntity journalEntry) {
        this.journalEntry = journalEntry;
    }

    public AccountEntity getAccount() {
        return account;
    }

    public void setAccount(AccountEntity account) {
        this.account = account;
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
