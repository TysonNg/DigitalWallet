package com.tyson.digitalwallet.ddd.application.usecase.admin.impl;

import com.tyson.digitalwallet.ddd.application.exception.WalletNotFoundException;
import com.tyson.digitalwallet.ddd.application.usecase.admin.ReconcileWalletUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.admin.response.WalletReconciliationResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import com.tyson.digitalwallet.ddd.domain.service.ledger.LedgerService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ReconcileWalletUseCaseImpl implements ReconcileWalletUseCase {

    private final WalletRepository walletRepository;
    private final LedgerService ledgerService;

    public ReconcileWalletUseCaseImpl(WalletRepository walletRepository, LedgerService ledgerService) {
        this.walletRepository = walletRepository;
        this.ledgerService = ledgerService;
    }

    @Override
    public WalletReconciliationResponse reconcile(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));

        boolean reconciled = ledgerService.reconcileWallet(walletId, wallet.getBalance());
        return new WalletReconciliationResponse(
                walletId,
                wallet.getBalance(),
                reconciled
        );
    }
}
