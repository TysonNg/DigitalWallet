package com.tyson.digitalwallet.ddd.application.usecase.admin;

import com.tyson.digitalwallet.ddd.application.usecase.admin.response.WalletReconciliationResponse;

import java.util.UUID;

public interface ReconcileWalletUseCase {
    WalletReconciliationResponse reconcile(UUID walletId);
}
