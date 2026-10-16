package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.exception.AppException;
import com.tyson.digitalwallet.ddd.application.exception.ErrorCode;
import com.tyson.digitalwallet.ddd.application.port.security.PasswordHasher;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.ChangeWalletPinUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.ChangeWalletPinCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.pin.WalletPinAttemptService;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.pin.WalletPinValidator;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeWalletPinUseCaseImpl implements ChangeWalletPinUseCase {

    private final WalletRepository walletRepository;
    private final PasswordHasher passwordHasher;
    private final WalletPinAttemptService pinAttemptService;

    public ChangeWalletPinUseCaseImpl(
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
    public void changePin(ChangeWalletPinCommand command) {
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

        if (!wallet.hasPin()) {
            throw new AppException(ErrorCode.WALLET_PIN_NOT_SET, "Ví chưa thiết lập mã PIN. Vui lòng thiết lập mã PIN trước");
        }

        pinAttemptService.assertNotLocked(wallet.getId());

        if (!passwordHasher.matches(command.oldPin(), wallet.getPinHash())) {
            int remaining = pinAttemptService.recordFailedAttempt(wallet.getId());
            throw new AppException(ErrorCode.INVALID_WALLET_PIN,
                    "Mã PIN hiện tại không chính xác. Bạn còn " + remaining + " lần thử");
        }

        pinAttemptService.resetAttempts(wallet.getId());

        WalletPinValidator.validatePin(command.newPin());

        if (passwordHasher.matches(command.newPin(), wallet.getPinHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Mã PIN mới không được trùng với mã PIN hiện tại");
        }

        String hashedNewPin = passwordHasher.hash(command.newPin());
        wallet.changePin(hashedNewPin);
        walletRepository.saveWallet(wallet);
    }
}
