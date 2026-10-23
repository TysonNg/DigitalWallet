package com.tyson.digitalwallet.ddd.infrastructure.scheduler;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.ReconcileWithdrawalUseCase;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WithdrawalReconciliationScheduler {
    private final ReconcileWithdrawalUseCase withdrawals;

    public WithdrawalReconciliationScheduler(ReconcileWithdrawalUseCase withdrawals) {
        this.withdrawals = withdrawals;
    }

    @Scheduled(fixedDelay = 15000, initialDelay = 5000)
    public void reconcile() {
        withdrawals.reconcile();
    }
}
