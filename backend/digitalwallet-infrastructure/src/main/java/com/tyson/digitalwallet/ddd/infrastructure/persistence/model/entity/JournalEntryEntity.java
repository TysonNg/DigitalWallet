package com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
public class JournalEntryEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "payment_order_id")
    private UUID paymentOrderId;

    @Column(name = "description", nullable = false)
    private String description;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LedgerEntryEntity> entries = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public JournalEntryEntity() {
    }

    public JournalEntryEntity(UUID id, UUID transactionId, UUID paymentOrderId, String description, LocalDateTime createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.paymentOrderId = paymentOrderId;
        this.description = description;
        this.createdAt = createdAt;
    }

    public void addEntry(LedgerEntryEntity entry) {
        entries.add(entry);
        entry.setJournalEntry(this);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public UUID getPaymentOrderId() {
        return paymentOrderId;
    }

    public void setPaymentOrderId(UUID paymentOrderId) {
        this.paymentOrderId = paymentOrderId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<LedgerEntryEntity> getEntries() {
        return entries;
    }

    public void setEntries(List<LedgerEntryEntity> entries) {
        this.entries = entries;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
