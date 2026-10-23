package com.tyson.digitalwallet.ddd.domain.service.ledger;

import com.tyson.digitalwallet.ddd.domain.model.entity.Account;
import com.tyson.digitalwallet.ddd.domain.model.entity.JournalEntry;
import com.tyson.digitalwallet.ddd.domain.model.vo.TrialBalance;

import java.math.BigDecimal;
import java.util.UUID;

public interface LedgerService {
    Account getOrCreateWalletAccount(UUID walletId, String currency);

    JournalEntry recordDeposit(UUID transactionId, UUID paymentOrderId, UUID walletId, BigDecimal amount, String gatewayCode, BigDecimal fee, String description);

    JournalEntry recordWithdraw(UUID transactionId, UUID walletId, BigDecimal amount, BigDecimal fee, String description);

    JournalEntry recordWithdrawReturn(UUID transactionId, UUID walletId, BigDecimal amount, String description);

    JournalEntry recordTransfer(UUID transactionId, UUID senderWalletId, UUID receiverWalletId, BigDecimal amount, BigDecimal fee, String description);

    TrialBalance getTrialBalance();

    boolean reconcileWallet(UUID walletId, BigDecimal currentBalance);
}
