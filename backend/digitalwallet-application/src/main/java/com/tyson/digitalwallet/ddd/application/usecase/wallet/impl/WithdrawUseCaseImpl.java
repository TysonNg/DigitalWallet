package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.exception.WalletNotFoundException;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.WithdrawUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.WithdrawCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.Transaction;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.repository.TransactionRepository;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import com.tyson.digitalwallet.ddd.domain.event.MoneyWithdrawnEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class WithdrawUseCaseImpl implements WithdrawUseCase {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public WithdrawUseCaseImpl(
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
    public WalletResponse withdraw(WithdrawCommand command) {
        UUID walletId = command.walletId();
        BigDecimal amount = command.amount();

        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Not found wallet with id " + walletId));

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.withdraw(amount);
        BigDecimal balanceAfter = wallet.getBalance();

        Wallet savedWallet = walletRepository.saveWallet(wallet);

        Transaction transaction = Transaction.createWithdraw(walletId, amount, balanceBefore, balanceAfter);
        Transaction savedTransaction = transactionRepository.save(transaction);

        eventPublisher.publishEvent(new MoneyWithdrawnEvent(
                savedTransaction.getId(),
                wallet.getUserId(),
                amount,
                balanceAfter,
                Instant.now()
        ));

        return WalletResponse.from(savedWallet);
    }
}
