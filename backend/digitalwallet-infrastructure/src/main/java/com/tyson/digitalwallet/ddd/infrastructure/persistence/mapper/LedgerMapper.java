package com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper;

import com.tyson.digitalwallet.ddd.domain.model.entity.Account;
import com.tyson.digitalwallet.ddd.domain.model.entity.JournalEntry;
import com.tyson.digitalwallet.ddd.domain.model.entity.LedgerEntry;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.AccountEntity;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.JournalEntryEntity;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.LedgerEntryEntity;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.WalletEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class LedgerMapper {

    public Account toDomain(AccountEntity entity) {
        if (entity == null) {
            return null;
        }
        UUID walletId = entity.getWallet() != null ? entity.getWallet().getId() : null;
        return new Account(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getType(),
                entity.getCurrency(),
                walletId,
                entity.getCreatedAt()
        );
    }

    public AccountEntity toEntity(Account domain) {
        if (domain == null) {
            return null;
        }
        WalletEntity walletEntity = null;
        if (domain.getWalletId() != null) {
            walletEntity = new WalletEntity();
            walletEntity.setId(domain.getWalletId());
        }
        return new AccountEntity(
                domain.getId(),
                domain.getCode(),
                domain.getName(),
                domain.getType(),
                domain.getCurrency(),
                walletEntity,
                domain.getCreatedAt()
        );
    }

    public LedgerEntry toDomain(LedgerEntryEntity entity) {
        if (entity == null) {
            return null;
        }
        return new LedgerEntry(
                entity.getId(),
                entity.getJournalEntry() != null ? entity.getJournalEntry().getId() : null,
                entity.getAccount() != null ? entity.getAccount().getId() : null,
                entity.getDebitAmount(),
                entity.getCreditAmount(),
                entity.getCreatedAt()
        );
    }

    public JournalEntry toDomain(JournalEntryEntity entity) {
        if (entity == null) {
            return null;
        }
        JournalEntry journal = new JournalEntry(
                entity.getId(),
                entity.getTransactionId(),
                entity.getPaymentOrderId(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
        if (entity.getEntries() != null) {
            journal.setEntries(entity.getEntries().stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList()));
        }
        return journal;
    }
}
