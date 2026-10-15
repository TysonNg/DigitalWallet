package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.WithdrawUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.WithdrawCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.domain.annotation.DistributedLock;
import com.tyson.digitalwallet.ddd.domain.event.MoneyWithdrawnEvent;
import com.tyson.digitalwallet.ddd.domain.service.wallet.WalletService;
import com.tyson.digitalwallet.ddd.domain.service.wallet.result.WithdrawResult;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class WithdrawUseCaseImpl implements WithdrawUseCase {

    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;

    public WithdrawUseCaseImpl(
            WalletService walletService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.walletService = walletService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @DistributedLock(
            key = "'lock:wallet:withdraw:' + #command.walletId()",
            waitTime = 0,
            leaseTime = 10,
            message = "Giao dịch rút tiền đang được xử lý, vui lòng chờ trong giây lát"
    )
    @Transactional
    public WalletResponse withdraw(WithdrawCommand command) {
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
