package com.tyson.digitalwallet.ddd.domain.model.entity;

import com.tyson.digitalwallet.ddd.domain.exception.UnbalancedLedgerException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class JournalEntry {
    private UUID id;
    private UUID transactionId;
    private UUID paymentOrderId;
    private String description;
    private List<LedgerEntry> entries = new ArrayList<>();
    private LocalDateTime createdAt;

    public JournalEntry() {
    }

    public JournalEntry(UUID id, UUID transactionId, UUID paymentOrderId, String description, LocalDateTime createdAt) {
        this.id = (id != null) ? id : UUID.randomUUID();
        this.transactionId = transactionId;
        this.paymentOrderId = paymentOrderId;
        this.description = description;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    public void addDebit(UUID accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        this.entries.add(LedgerEntry.debit(this.id, accountId, amount));
    }

    public void addCredit(UUID accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        this.entries.add(LedgerEntry.credit(this.id, accountId, amount));
    }

    /**
     * Quy tắc bất biến (Domain Invariant):
     * Tổng số tiền ghi Nợ (Debit) BẮT BUỘC PHẢI BẰNG Tổng số tiền ghi Có (Credit).
     */
    public void validate() {
        if (entries == null || entries.size() < 2) {
            throw new UnbalancedLedgerException("Bút toán sổ cái không hợp lệ: Cần ít nhất 2 dòng đối ứng (Debit/Credit)");
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {
            totalDebit = totalDebit.add(entry.getDebitAmount());
            totalCredit = totalCredit.add(entry.getCreditAmount());
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new UnbalancedLedgerException(
                    String.format("Lệch sổ cái! Tổng Nợ (%s) không bằng Tổng Có (%s). Chênh lệch: %s",
                            totalDebit, totalCredit, totalDebit.subtract(totalCredit))
            );
        }
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

    public List<LedgerEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public void setEntries(List<LedgerEntry> entries) {
        this.entries = (entries != null) ? new ArrayList<>(entries) : new ArrayList<>();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
