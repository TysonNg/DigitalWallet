package com.tyson.digitalwallet.ddd.domain.service.transaction.impl;

import com.tyson.digitalwallet.ddd.domain.model.entity.Transaction;
import com.tyson.digitalwallet.ddd.domain.repository.TransactionRepository;
import com.tyson.digitalwallet.ddd.domain.service.transaction.TransactionService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Transaction recordDeposit(UUID walletId, BigDecimal amount, BigDecimal before, BigDecimal after) {
        Transaction tx = Transaction.createDeposit(walletId, amount, before, after);
        return transactionRepository.save(tx);
    }

    @Override
    public Transaction recordWithdraw(UUID walletId, BigDecimal amount, BigDecimal before, BigDecimal after) {
        Transaction tx = Transaction.createWithdraw(walletId, amount, before, after);
        return transactionRepository.save(tx);
    }

    @Override
    public Transaction recordTransfer(UUID senderWalletId, UUID receiverWalletId, BigDecimal amount, BigDecimal before, BigDecimal after, String description) {
        Transaction tx = Transaction.createTransfer(senderWalletId, receiverWalletId, amount, before, after, description);
        return transactionRepository.save(tx);
    }

    @Override
    public List<Transaction> getTransactionsByWallet(UUID walletId) {
        return transactionRepository.findByWalletId(walletId);
    }

    @Override
    public Transaction getById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with id: " + id));
    }
}
