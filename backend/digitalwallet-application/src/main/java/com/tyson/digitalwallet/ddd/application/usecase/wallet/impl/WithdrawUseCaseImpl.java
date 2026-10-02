package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.exception.WalletNotFoundException;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.WithdrawUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.WithdrawCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WithdrawUseCaseImpl implements WithdrawUseCase {

    WalletRepository walletRepository;


    public WithdrawUseCaseImpl(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional
    public WalletResponse withdraw(WithdrawCommand command) {
        UUID walletId = command.walletId();
        BigDecimal amount = command.amount();

        Wallet wallet = walletRepository.findById(walletId).orElseThrow(() -> new WalletNotFoundException("Not found wallet with id " + walletId));

        wallet.withdraw(amount);

        Wallet savedWallet = walletRepository.saveWallet(wallet);

        return WalletResponse.from(savedWallet);
    };
}
