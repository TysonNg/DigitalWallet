package com.tyson.digitalwallet.ddd.application.usecase.transaction.impl;

import com.tyson.digitalwallet.ddd.application.usecase.transaction.GetTransactionsUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.transaction.response.TransactionResponse;
import com.tyson.digitalwallet.ddd.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetTransactionsUseCaseImpl implements GetTransactionsUseCase {

    private final TransactionRepository transactionRepository;

    public GetTransactionsUseCaseImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public List<TransactionResponse> getTransactionsByWalletId(UUID walletId) {
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID must not be null");
        }
        return transactionRepository.findByWalletId(walletId).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Override
    public TransactionResponse getTransactionById(UUID transactionId) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID must not be null");
        }
        return transactionRepository.findById(transactionId)
                .map(TransactionResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found with id: " + transactionId));
    }
}
