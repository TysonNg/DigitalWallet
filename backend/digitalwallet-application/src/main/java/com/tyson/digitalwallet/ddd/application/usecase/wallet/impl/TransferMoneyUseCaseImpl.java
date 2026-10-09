package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.exception.WalletNotFoundException;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.TransferMoneyUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.TransferMoneyCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.application.util.RequestHashUtil;
import com.tyson.digitalwallet.ddd.domain.model.entity.IdempotencyKey;
import com.tyson.digitalwallet.ddd.domain.model.entity.Transaction;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.repository.IdempotencyKeyRepository;
import com.tyson.digitalwallet.ddd.domain.repository.TransactionRepository;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import com.tyson.digitalwallet.ddd.domain.event.MoneyTransferEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferMoneyUseCaseImpl implements TransferMoneyUseCase {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TransferMoneyUseCaseImpl(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            IdempotencyKeyRepository idempotencyKeyRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public WalletResponse transfer(TransferMoneyCommand command) {
        UUID senderWalletId = command.senderWalletId();
        UUID receiverWalletId = command.receiverWalletId();
        BigDecimal amount = command.amount();

        if (senderWalletId == null || receiverWalletId == null) {
            throw new IllegalArgumentException("Sender and receiver wallet IDs must not be null");
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

        // 2. Deadlock-free Pessimistic Locking
        // Sort wallet IDs deterministically to prevent deadlocks across concurrent transfers
        UUID firstLockId = senderWalletId.compareTo(receiverWalletId) < 0 ? senderWalletId : receiverWalletId;
        UUID secondLockId = senderWalletId.compareTo(receiverWalletId) < 0 ? receiverWalletId : senderWalletId;

        Wallet firstWallet = walletRepository.findByIdWithLock(firstLockId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + firstLockId));
        Wallet secondWallet = walletRepository.findByIdWithLock(secondLockId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + secondLockId));

        Wallet senderWallet = senderWalletId.equals(firstWallet.getId()) ? firstWallet : secondWallet;
        Wallet receiverWallet = receiverWalletId.equals(firstWallet.getId()) ? firstWallet : secondWallet;

        // 3. Handle Idempotency Key (if provided by client)
        IdempotencyKey idempotencyRecord = null;
        if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
            UUID userId = senderWallet.getUserId();
            Optional<IdempotencyKey> existingKeyOpt = idempotencyKeyRepository.findByUserIdAndKey(userId, command.idempotencyKey());

            if (existingKeyOpt.isPresent()) {
                IdempotencyKey existingKey = existingKeyOpt.get();

                // Check for payload mismatch (Key reuse with different parameters)
                if (!existingKey.getRequestHash().equals(requestHash)) {
                    throw new IllegalStateException("Idempotency key has already been used with different transfer parameters!");
                }

                if (existingKey.isCompleted()) {
                    // Return the existing result without re-executing
                    return WalletResponse.from(senderWallet);
                }

                if (existingKey.isProcessing()) {
                    throw new IllegalStateException("A transfer with this idempotency key is currently processing. Please wait.");
                }

                if (!existingKey.isExpired()) {
                    throw new IllegalStateException("Previous transfer attempt with this idempotency key failed.");
                }
            }

            // Start new processing state
            idempotencyRecord = IdempotencyKey.startProcessing(userId, command.idempotencyKey(), requestHash);
            idempotencyRecord = idempotencyKeyRepository.save(idempotencyRecord);
        }

        // 4. Execute Balance Changes (Rich Domain Entity logic)
        BigDecimal senderBalanceBefore = senderWallet.getBalance();
        senderWallet.withdraw(amount);
        BigDecimal senderBalanceAfter = senderWallet.getBalance();

        receiverWallet.deposit(amount);

        // 5. Persist Wallets
        Wallet savedSenderWallet = walletRepository.saveWallet(senderWallet);
        walletRepository.saveWallet(receiverWallet);

        // 6. Record Transaction Ledger
        Transaction transaction = Transaction.createTransfer(
                senderWalletId,
                receiverWalletId,
                amount,
                senderBalanceBefore,
                senderBalanceAfter,
                command.description()
        );
        Transaction savedTransaction = transactionRepository.save(transaction);

        // 7. Complete Idempotency Record
        if (idempotencyRecord != null) {
            idempotencyRecord.complete(savedTransaction.getId());
            idempotencyKeyRepository.save(idempotencyRecord);
        }

        // 8. Publish MoneyTransferEvent
        eventPublisher.publishEvent(new MoneyTransferEvent(
                savedTransaction.getId(),
                senderWallet.getUserId(),
                receiverWallet.getUserId(),
                amount,
                senderBalanceAfter,
                command.description(),
                Instant.now()
        ));

        return WalletResponse.from(savedSenderWallet);
    }
}
