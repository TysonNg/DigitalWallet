package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.JournalEntry;
import com.tyson.digitalwallet.ddd.domain.model.entity.LedgerEntry;
import com.tyson.digitalwallet.ddd.domain.model.vo.TrialBalance;
import com.tyson.digitalwallet.ddd.domain.repository.LedgerRepository;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.LedgerMapper;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.AccountEntity;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.JournalEntryEntity;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.LedgerEntryEntity;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class LedgerRepositoryImpl implements LedgerRepository {

    private final SpringDataJournalEntryRepository springDataJournalEntryRepository;
    private final SpringDataLedgerEntryRepository springDataLedgerEntryRepository;
    private final SpringDataAccountRepository springDataAccountRepository;
    private final LedgerMapper ledgerMapper;

    public LedgerRepositoryImpl(
            SpringDataJournalEntryRepository springDataJournalEntryRepository,
            SpringDataLedgerEntryRepository springDataLedgerEntryRepository,
            SpringDataAccountRepository springDataAccountRepository,
            LedgerMapper ledgerMapper
    ) {
        this.springDataJournalEntryRepository = springDataJournalEntryRepository;
        this.springDataLedgerEntryRepository = springDataLedgerEntryRepository;
        this.springDataAccountRepository = springDataAccountRepository;
        this.ledgerMapper = ledgerMapper;
    }

    @Override
    public JournalEntry saveJournal(JournalEntry journalEntry) {
        JournalEntryEntity journalEntity = new JournalEntryEntity(
                journalEntry.getId(),
                journalEntry.getTransactionId(),
                journalEntry.getPaymentOrderId(),
                journalEntry.getDescription(),
                journalEntry.getCreatedAt()
        );

        if (journalEntry.getEntries() != null) {
            for (LedgerEntry entry : journalEntry.getEntries()) {
                AccountEntity accountRef = springDataAccountRepository.getReferenceById(entry.getAccountId());
                LedgerEntryEntity entryEntity = new LedgerEntryEntity(
                        entry.getId(),
                        journalEntity,
                        accountRef,
                        entry.getDebitAmount(),
                        entry.getCreditAmount(),
                        entry.getCreatedAt()
                );
                journalEntity.addEntry(entryEntity);
            }
        }

        JournalEntryEntity saved = springDataJournalEntryRepository.save(journalEntity);
        return ledgerMapper.toDomain(saved);
    }

    @Override
    public Optional<JournalEntry> findJournalById(UUID id) {
        return springDataJournalEntryRepository.findById(id)
                .map(ledgerMapper::toDomain);
    }

    @Override
    public List<LedgerEntry> findEntriesByAccountId(UUID accountId) {
        return springDataLedgerEntryRepository.findByAccount_IdOrderByCreatedAtDesc(accountId).stream()
                .map(ledgerMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public TrialBalance getTrialBalance() {
        BigDecimal totalDebit = springDataLedgerEntryRepository.sumTotalDebit();
        BigDecimal totalCredit = springDataLedgerEntryRepository.sumTotalCredit();
        long count = springDataLedgerEntryRepository.count();
        return TrialBalance.of(totalDebit, totalCredit, count);
    }

    @Override
    public BigDecimal calculateAccountBalance(UUID accountId) {
        return springDataLedgerEntryRepository.calculateBalanceForLiabilityAccount(accountId);
    }
}
