package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.WithdrawUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.WithdrawCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.domain.event.MoneyWithdrawnEvent;
import com.tyson.digitalwallet.ddd.application.port.lock.DistributedLockService;
import com.tyson.digitalwallet.ddd.domain.service.wallet.WalletService;
import com.tyson.digitalwallet.ddd.domain.service.wallet.result.WithdrawResult;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import com.tyson.digitalwallet.ddd.application.exception.AppException;
import com.tyson.digitalwallet.ddd.application.exception.ErrorCode;
import com.tyson.digitalwallet.ddd.application.port.security.PasswordHasher;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.pin.WalletPinAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import com.tyson.digitalwallet.ddd.domain.service.ledger.LedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class WithdrawUseCaseImpl implements WithdrawUseCase {

    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final DistributedLockService lockService;
    private final TransactionTemplate transactionTemplate;
    private final PasswordHasher passwordHasher;
    private final WalletPinAttemptService pinAttemptService;
    private final LedgerService ledgerService;

    public WithdrawUseCaseImpl(
            WalletService walletService,
            ApplicationEventPublisher eventPublisher,
            DistributedLockService lockService,
            TransactionTemplate transactionTemplate
    ) {
        this(walletService, null, eventPublisher, lockService, transactionTemplate, null, null, null);
    }

    public WithdrawUseCaseImpl(
            WalletService walletService,
            WalletRepository walletRepository,
            ApplicationEventPublisher eventPublisher,
            DistributedLockService lockService,
            TransactionTemplate transactionTemplate,
            PasswordHasher passwordHasher,
            WalletPinAttemptService pinAttemptService
    ) {
        this(walletService, walletRepository, eventPublisher, lockService, transactionTemplate, passwordHasher, pinAttemptService, null);
    }

    @Autowired
    public WithdrawUseCaseImpl(
            WalletService walletService,
            WalletRepository walletRepository,
            ApplicationEventPublisher eventPublisher,
            DistributedLockService lockService,
            TransactionTemplate transactionTemplate,
            PasswordHasher passwordHasher,
            WalletPinAttemptService pinAttemptService,
            LedgerService ledgerService
    ) {
        this.walletService = walletService;
        this.walletRepository = walletRepository;
        this.eventPublisher = eventPublisher;
        this.lockService = lockService;
        this.transactionTemplate = transactionTemplate;
        this.passwordHasher = passwordHasher;
        this.pinAttemptService = pinAttemptService;
        this.ledgerService = ledgerService;
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
        if (walletRepository != null && passwordHasher != null && pinAttemptService != null) {
            Wallet wallet = walletRepository.findById(command.walletId()).orElse(null);
            if (wallet != null && wallet.hasPin()) {
                pinAttemptService.assertNotLocked(command.walletId());
                if (command.pin() == null || command.pin().isBlank()) {
                    throw new AppException(
                            ErrorCode.INVALID_WALLET_PIN,
                            "Mã PIN là bắt buộc để thực hiện rút tiền"
                    );
                }
                if (!passwordHasher.matches(command.pin(), wallet.getPinHash())) {
                    int remaining = pinAttemptService.recordFailedAttempt(command.walletId());
                    throw new AppException(
                            ErrorCode.INVALID_WALLET_PIN,
                            "Mã PIN không chính xác. Bạn còn " + remaining + " lần thử"
                    );
                }
                pinAttemptService.resetAttempts(command.walletId());
            }
        }

        WithdrawResult result = walletService.withdraw(command.walletId(), command.amount());

        if (ledgerService != null) {
            ledgerService.recordWithdraw(
                    result.transaction().getId(),
                    command.walletId(),
                    command.amount(),
                    BigDecimal.ZERO,
                    "Rút tiền từ ví #" + command.walletId()
            );
        }

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
