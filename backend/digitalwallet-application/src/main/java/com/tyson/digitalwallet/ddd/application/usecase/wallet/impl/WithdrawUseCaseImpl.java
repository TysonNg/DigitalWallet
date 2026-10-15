package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.WithdrawUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.WithdrawCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.domain.event.MoneyWithdrawnEvent;
import com.tyson.digitalwallet.ddd.domain.service.lock.DistributedLockService;
import com.tyson.digitalwallet.ddd.domain.service.wallet.WalletService;
import com.tyson.digitalwallet.ddd.domain.service.wallet.result.WithdrawResult;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

@Service
public class WithdrawUseCaseImpl implements WithdrawUseCase {

    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;
    private final DistributedLockService lockService;
    private final TransactionTemplate transactionTemplate;

    public WithdrawUseCaseImpl(
            WalletService walletService,
            ApplicationEventPublisher eventPublisher,
            DistributedLockService lockService,
            TransactionTemplate transactionTemplate
    ) {
        this.walletService = walletService;
        this.eventPublisher = eventPublisher;
        this.lockService = lockService;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public WalletResponse withdraw(WithdrawCommand command) {
        UUID walletId = command.walletId();
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID must not be null");
        }

        String lockKey = "lock:wallet:withdraw:" + walletId;
        return lockService.executeWithLock(lockKey, () ->
                transactionTemplate.execute(status -> doWithdrawInternal(command))
        );
    }

    private WalletResponse doWithdrawInternal(WithdrawCommand command) {
        WithdrawResult result = walletService.withdraw(command.walletId(), command.amount());

        eventPublisher.publishEvent(new MoneyWithdrawnEvent(
                result.transaction().getId(),
                result.wallet().getUserId(),
                command.amount(),
                result.wallet().getBalance(),
                Instant.now()
        ));

        return WalletResponse.from(result.wallet());
    }
}
