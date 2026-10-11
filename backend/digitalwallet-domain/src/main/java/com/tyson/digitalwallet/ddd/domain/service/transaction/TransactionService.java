package com.tyson.digitalwallet.ddd.domain.service.transaction;

import com.tyson.digitalwallet.ddd.domain.model.entity.Transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TransactionService {
    Transaction recordDeposit(UUID walletId, BigDecimal amount, BigDecimal before, BigDecimal after);

    Transaction recordWithdraw(UUID walletId, BigDecimal amount, BigDecimal before, BigDecimal after);

    Transaction recordTransfer(UUID senderWalletId, UUID receiverWalletId, BigDecimal amount, BigDecimal before, BigDecimal after, String description);

    List<Transaction> getTransactionsByWallet(UUID walletId);

    Transaction getById(UUID id);
}
