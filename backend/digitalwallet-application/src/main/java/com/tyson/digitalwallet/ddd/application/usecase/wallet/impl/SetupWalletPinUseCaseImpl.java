package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.exception.AppException;
import com.tyson.digitalwallet.ddd.application.exception.ErrorCode;
import com.tyson.digitalwallet.ddd.application.port.security.PasswordHasher;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.SetupWalletPinUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.SetupWalletPinCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.pin.WalletPinAttemptService;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.pin.WalletPinValidator;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetupWalletPinUseCaseImpl implements SetupWalletPinUseCase {

    private final WalletRepository walletRepository;
    private final PasswordHasher passwordHasher;
    private final WalletPinAttemptService pinAttemptService;

    public SetupWalletPinUseCaseImpl(
            WalletRepository walletRepository,
            PasswordHasher passwordHasher,
            WalletPinAttemptService pinAttemptService
    ) {
        this.walletRepository = walletRepository;
        this.passwordHasher = passwordHasher;
        this.pinAttemptService = pinAttemptService;
    }

    @Override
    @Transactional
    public void setupPin(SetupWalletPinCommand command) {
        if (command == null || command.userId() == null) {
            throw new AppException(ErrorCode.FORBIDDEN_ACCESS, "User ID is required");
        }

        Wallet wallet = (command.walletId() != null)
                ? walletRepository.findById(command.walletId())
                        .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND, "Wallet not found"))
                : walletRepository.findByUserId(command.userId())
                        .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND, "Wallet not found for current user"));

        if (!wallet.getUserId().equals(command.userId())) {
            throw new AppException(ErrorCode.FORBIDDEN_ACCESS, "You do not own this wallet");
        }

        if (wallet.hasPin()) {
            throw new AppException(ErrorCode.WALLET_ALREADY_HAS_PIN, "Ví đã được thiết lập mã PIN. Vui lòng sử dụng tính năng đổi mã PIN");
        }

        WalletPinValidator.validatePin(command.pin());

        String hashedPin = passwordHasher.hash(command.pin());
        wallet.setupPin(hashedPin);
        walletRepository.saveWallet(wallet);

        pinAttemptService.resetAttempts(wallet.getId());
    }
}
