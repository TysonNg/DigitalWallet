package com.tyson.digitalwallet.ddd.domain.service.wallet.impl;

import com.tyson.digitalwallet.ddd.domain.model.entity.Transaction;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;
import com.tyson.digitalwallet.ddd.domain.repository.TransactionRepository;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import com.tyson.digitalwallet.ddd.domain.service.wallet.WalletService;
import com.tyson.digitalwallet.ddd.domain.service.wallet.result.DepositResult;
import com.tyson.digitalwallet.ddd.domain.service.wallet.result.TransferResult;
import com.tyson.digitalwallet.ddd.domain.service.wallet.result.WithdrawResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletServiceImpl(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Wallet createWallet(UUID userId, String currency) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (walletRepository.existsByUserId(userId)) {
            throw new IllegalStateException("User already has an active wallet");
        }

        String finalCurrency = (currency != null && !currency.isBlank()) ? currency : "VND";
        Wallet wallet = new Wallet(
                UUID.randomUUID(),
                userId,
                BigDecimal.ZERO,
                finalCurrency,
                WalletStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        return walletRepository.saveWallet(wallet);
    }

    @Override
    public DepositResult deposit(UUID walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.deposit(amount);
        BigDecimal balanceAfter = wallet.getBalance();

        Wallet savedWallet = walletRepository.saveWallet(wallet);

        Transaction transaction = Transaction.createDeposit(walletId, amount, balanceBefore, balanceAfter);
        Transaction savedTransaction = transactionRepository.save(transaction);

        return new DepositResult(savedWallet, savedTransaction);
    }

    @Override
    public WithdrawResult withdraw(UUID walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.withdraw(amount);
        BigDecimal balanceAfter = wallet.getBalance();

        Wallet savedWallet = walletRepository.saveWallet(wallet);

        Transaction transaction = Transaction.createWithdraw(walletId, amount, balanceBefore, balanceAfter);
        Transaction savedTransaction = transactionRepository.save(transaction);

        return new WithdrawResult(savedWallet, savedTransaction);
    }

    @Override
    public TransferResult transfer(UUID senderWalletId, UUID receiverWalletId, BigDecimal amount, String description) {
        if (senderWalletId == null || receiverWalletId == null) {
            throw new IllegalArgumentException("Sender and receiver wallet IDs must not be null");
        }
        if (senderWalletId.equals(receiverWalletId)) {
            throw new IllegalArgumentException("Cannot transfer money to the same wallet");
        }

        // Chống Deadlock: Luôn lock theo thứ tự UUID cố định
        UUID firstId = senderWalletId.compareTo(receiverWalletId) < 0 ? senderWalletId : receiverWalletId;
        UUID secondId = senderWalletId.compareTo(receiverWalletId) < 0 ? receiverWalletId : senderWalletId;

        walletRepository.findByIdWithLock(firstId);
        walletRepository.findByIdWithLock(secondId);

        Wallet sender = walletRepository.findById(senderWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Sender wallet not found: " + senderWalletId));
        Wallet receiver = walletRepository.findById(receiverWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver wallet not found: " + receiverWalletId));

        BigDecimal senderBefore = sender.getBalance();
        BigDecimal receiverBefore = receiver.getBalance();

        sender.withdraw(amount);
        receiver.deposit(amount);

        Wallet savedSender = walletRepository.saveWallet(sender);
        Wallet savedReceiver = walletRepository.saveWallet(receiver);

        String desc = (description != null && !description.isBlank()) ? description : "Chuyển tiền ví";
        Transaction transaction = Transaction.createTransfer(
                senderWalletId,
                receiverWalletId,
                amount,
                senderBefore,
                savedSender.getBalance(),
                desc
        );
        Transaction savedTx = transactionRepository.save(transaction);

        return new TransferResult(savedSender, savedReceiver, savedTx);
    }
}
