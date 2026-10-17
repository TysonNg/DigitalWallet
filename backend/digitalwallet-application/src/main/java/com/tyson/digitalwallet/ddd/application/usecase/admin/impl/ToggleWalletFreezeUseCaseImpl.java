package com.tyson.digitalwallet.ddd.application.usecase.admin.impl;

import com.tyson.digitalwallet.ddd.application.exception.UserNotFoundException;
import com.tyson.digitalwallet.ddd.application.exception.WalletNotFoundException;
import com.tyson.digitalwallet.ddd.application.usecase.admin.ToggleWalletFreezeUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.admin.command.ToggleWalletFreezeCommand;
import com.tyson.digitalwallet.ddd.application.usecase.admin.response.AdminUserSummaryResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.Authority;
import com.tyson.digitalwallet.ddd.domain.model.entity.Role;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ToggleWalletFreezeUseCaseImpl implements ToggleWalletFreezeUseCase {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public ToggleWalletFreezeUseCaseImpl(WalletRepository walletRepository, UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public AdminUserSummaryResponse toggleFreeze(ToggleWalletFreezeCommand command) {
        Wallet wallet = walletRepository.findById(command.walletId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + command.walletId()));

        if (wallet.getStatus() == WalletStatus.ACTIVE) {
            wallet.frozen();
        } else if (wallet.getStatus() == WalletStatus.FROZEN) {
            wallet.activate();
        } else {
            throw new IllegalStateException("Closed wallet cannot be frozen or activated");
        }

        Wallet savedWallet = walletRepository.saveWallet(wallet);

        User user = userRepository.findById(savedWallet.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + savedWallet.getUserId()));

        Set<String> roles = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
                : Set.of();

        Set<String> authorities = new HashSet<>();
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                if (role.getAuthorities() != null) {
                    for (Authority auth : role.getAuthorities()) {
                        authorities.add(auth.getName());
                    }
                }
            }
        }

        BigDecimal balance = savedWallet.getBalance() != null ? savedWallet.getBalance() : BigDecimal.ZERO;
        String currency = savedWallet.getCurrency() != null ? savedWallet.getCurrency() : "VND";

        return new AdminUserSummaryResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getDob(),
                user.getStatus(),
                roles,
                authorities,
                savedWallet.getId(),
                savedWallet.getStatus(),
                balance,
                currency,
                user.getCreatedAt()
        );
    }
}
