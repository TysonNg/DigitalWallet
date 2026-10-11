package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.CreateWalletUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.CreateWalletCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.domain.service.wallet.WalletService;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class CreateWalletUseCaseImpl implements CreateWalletUseCase {

    private final UserRepository userRepository;
    private final WalletService walletService;

    public CreateWalletUseCaseImpl(UserRepository userRepository, WalletService walletService) {
        this.userRepository = userRepository;
        this.walletService = walletService;
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

        Wallet savedWallet = walletService.createWallet(userId, currency);
        return WalletResponse.from(savedWallet);
    }
}
