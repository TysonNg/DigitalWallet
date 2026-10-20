package com.tyson.digitalwallet.ddd.domain.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.JournalEntry;
import com.tyson.digitalwallet.ddd.domain.model.entity.LedgerEntry;
import com.tyson.digitalwallet.ddd.domain.model.vo.TrialBalance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerRepository {
    JournalEntry saveJournal(JournalEntry journalEntry);

    Optional<JournalEntry> findJournalById(UUID id);

    List<LedgerEntry> findEntriesByAccountId(UUID accountId);

    TrialBalance getTrialBalance();

    BigDecimal calculateAccountBalance(UUID accountId);
}
