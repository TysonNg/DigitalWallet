package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.exception.WalletNotFoundException;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.TransferMoneyUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.TransferMoneyCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.application.util.RequestHashUtil;
import com.tyson.digitalwallet.ddd.domain.event.MoneyTransferEvent;
import com.tyson.digitalwallet.ddd.domain.model.entity.IdempotencyKey;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.repository.IdempotencyKeyRepository;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import com.tyson.digitalwallet.ddd.domain.service.lock.DistributedLockService;
import com.tyson.digitalwallet.ddd.domain.service.wallet.WalletService;
import com.tyson.digitalwallet.ddd.domain.service.wallet.result.TransferResult;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferMoneyUseCaseImpl implements TransferMoneyUseCase {

    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final DistributedLockService lockService;
    private final TransactionTemplate transactionTemplate;

    public TransferMoneyUseCaseImpl(
            WalletService walletService,
            WalletRepository walletRepository,
            IdempotencyKeyRepository idempotencyKeyRepository,
            ApplicationEventPublisher eventPublisher,
            DistributedLockService lockService,
            TransactionTemplate transactionTemplate
    ) {
        this.walletService = walletService;
        this.walletRepository = walletRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.eventPublisher = eventPublisher;
        this.lockService = lockService;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public WalletResponse transfer(TransferMoneyCommand command) {
        UUID senderWalletId = command.senderWalletId();
        if (senderWalletId == null) {
            throw new IllegalArgumentException("Sender wallet ID must not be null");
        }

        String lockKey = "lock:wallet:transfer:" + senderWalletId;
        return lockService.executeWithLock(lockKey, () ->
                transactionTemplate.execute(status -> doTransferInternal(command))
        );
    }

    private WalletResponse doTransferInternal(TransferMoneyCommand command) {
        UUID senderWalletId = command.senderWalletId();
        UUID receiverWalletId = command.receiverWalletId();
        BigDecimal amount = command.amount();

        if (receiverWalletId == null) {
            throw new IllegalArgumentException("Receiver wallet ID must not be null");
        }

        if (senderWalletId.equals(receiverWalletId)) {
            throw new IllegalArgumentException("Cannot transfer money to the same wallet");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        // 1. Calculate Request Hash
        String requestHash = RequestHashUtil.computeTransferHash(
                senderWalletId,
                receiverWalletId,
                amount,
                command.description()
        );

        // 2. Kiểm tra sender wallet để lấy userId cho IdempotencyKey
        Wallet senderWallet = walletRepository.findById(senderWalletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + senderWalletId));

        // 3. Handle Idempotency Key (nếu có từ client)
        IdempotencyKey idempotencyRecord = null;
        if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
            UUID userId = senderWallet.getUserId();
            Optional<IdempotencyKey> existingKeyOpt = idempotencyKeyRepository.findByUserIdAndKey(userId, command.idempotencyKey());

            if (existingKeyOpt.isPresent()) {
                IdempotencyKey existingKey = existingKeyOpt.get();

                if (!existingKey.getRequestHash().equals(requestHash)) {
                    throw new IllegalStateException("Idempotency key has already been used with different transfer parameters!");
                }

                if (existingKey.isCompleted()) {
                    return WalletResponse.from(senderWallet);
                }

                if (existingKey.isProcessing()) {
                    throw new IllegalStateException("A transfer with this idempotency key is currently processing. Please wait.");
                }

                if (!existingKey.isExpired()) {
                    throw new IllegalStateException("Previous transfer attempt with this idempotency key failed.");
                }
            }

            idempotencyRecord = IdempotencyKey.startProcessing(userId, command.idempotencyKey(), requestHash);
            idempotencyRecord = idempotencyKeyRepository.save(idempotencyRecord);
        }

        // 4. Ủy thác toàn bộ nghiệp vụ chuyển tiền đa thực thể cho WalletService (Domain Layer)
        TransferResult result = walletService.transfer(
                senderWalletId,
                receiverWalletId,
                amount,
                command.description()
        );

        // 5. Cập nhật Idempotency record
        if (idempotencyRecord != null) {
            idempotencyRecord.complete(result.transaction().getId());
            idempotencyKeyRepository.save(idempotencyRecord);
        }

        // 6. Publish MoneyTransferEvent (WebSocket STOMP sẽ tự động đẩy thông báo)
        eventPublisher.publishEvent(new MoneyTransferEvent(
                result.transaction().getId(),
                result.senderWallet().getUserId(),
                result.receiverWallet().getUserId(),
                amount,
                result.senderWallet().getBalance(),
                command.description(),
                Instant.now()
        ));

        return WalletResponse.from(result.senderWallet());
    }
}
