package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.CreateWalletUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.CreateWalletCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class CreateWalletUseCaseImpl implements CreateWalletUseCase {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public CreateWalletUseCaseImpl(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    public WalletResponse createWallet(CreateWalletCommand createWalletCommand) {
        UUID userId = createWalletCommand.userId();
        String currency = createWalletCommand.currency();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));

        if (!user.canCreateWallet()) {
            throw new IllegalArgumentException("User must be at least 18 years old to create a wallet");
        }

        if (walletRepository.existsByUserId(userId)) {
            throw new IllegalStateException("User already has a wallet: " + userId);
        }

        Wallet wallet = new Wallet(
                UUID.randomUUID(),
                userId,
                BigDecimal.ZERO,
                currency != null && !currency.isBlank() ? currency : "VND",
                WalletStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Wallet savedWallet = walletRepository.saveWallet(wallet);
        return WalletResponse.from(savedWallet);
    }
}
