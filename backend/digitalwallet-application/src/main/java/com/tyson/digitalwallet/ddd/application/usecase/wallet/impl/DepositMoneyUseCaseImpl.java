package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.DepositMoneyUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.DepositMoneyCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.domain.event.MoneyDepositEvent;
import com.tyson.digitalwallet.ddd.domain.service.wallet.result.DepositResult;
import com.tyson.digitalwallet.ddd.domain.service.wallet.WalletService;
import com.tyson.digitalwallet.ddd.domain.service.ledger.LedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class DepositMoneyUseCaseImpl implements DepositMoneyUseCase {

    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;
    private final LedgerService ledgerService;

    public DepositMoneyUseCaseImpl(
            WalletService walletService,
            ApplicationEventPublisher eventPublisher
    ) {
        this(walletService, eventPublisher, null);
    }

    @Autowired
    public DepositMoneyUseCaseImpl(
            WalletService walletService,
            ApplicationEventPublisher eventPublisher,
            LedgerService ledgerService
    ) {
        this.walletService = walletService;
        this.eventPublisher = eventPublisher;
        this.ledgerService = ledgerService;
    }

    @Override
    @Transactional
    public WalletResponse deposit(DepositMoneyCommand command) {
        DepositResult result = walletService.deposit(command.walletId(), command.amount());

        if (ledgerService != null) {
            ledgerService.recordDeposit(
                    result.transaction().getId(),
                    null,
                    command.walletId(),
                    command.amount(),
                    "BANK_VAULT",
                    BigDecimal.ZERO,
                    "Nạp tiền vào ví #" + command.walletId()
            );
        }

        eventPublisher.publishEvent(new MoneyDepositEvent(
                result.transaction().getId(),
                result.wallet().getUserId(),
                command.amount(),
                result.wallet().getBalance(),
                Instant.now()
        ));

        return WalletResponse.from(result.wallet());
    }
}
