package com.tyson.digitalwallet.ddd.application.usecase.transaction;

import com.tyson.digitalwallet.ddd.application.usecase.transaction.response.TransactionResponse;

import java.util.List;
import java.util.UUID;

public interface GetTransactionsUseCase {
    List<TransactionResponse> getTransactionsByWalletId(UUID walletId);
    TransactionResponse getTransactionById(UUID transactionId);
}
