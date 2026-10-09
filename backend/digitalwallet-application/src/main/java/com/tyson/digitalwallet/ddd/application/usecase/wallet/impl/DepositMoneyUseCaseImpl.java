package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.exception.WalletNotFoundException;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.DepositMoneyUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.DepositMoneyCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.Transaction;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.repository.TransactionRepository;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import com.tyson.digitalwallet.ddd.domain.event.MoneyDepositEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class DepositMoneyUseCaseImpl implements DepositMoneyUseCase {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DepositMoneyUseCaseImpl(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public WalletResponse deposit(DepositMoneyCommand command) {
        UUID walletId = command.walletId();
        BigDecimal amount = command.amount();

        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Not found wallet with id " + walletId));

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.deposit(amount);
        BigDecimal balanceAfter = wallet.getBalance();

        Wallet savedWallet = walletRepository.saveWallet(wallet);

        Transaction transaction = Transaction.createDeposit(walletId, amount, balanceBefore, balanceAfter);
        Transaction savedTransaction = transactionRepository.save(transaction);

        eventPublisher.publishEvent(new MoneyDepositEvent(
                savedTransaction.getId(),
                wallet.getUserId(),
                amount,
                balanceAfter,
                Instant.now()
        ));

        return WalletResponse.from(savedWallet);
    }
}
