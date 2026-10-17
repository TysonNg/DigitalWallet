package com.tyson.digitalwallet.ddd.application.usecase.admin.impl;

import com.tyson.digitalwallet.ddd.application.exception.UserNotFoundException;
import com.tyson.digitalwallet.ddd.application.port.security.TokenStorage;
import com.tyson.digitalwallet.ddd.application.usecase.admin.ToggleUserStatusUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.admin.command.ToggleUserStatusCommand;
import com.tyson.digitalwallet.ddd.application.usecase.admin.response.AdminUserSummaryResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.Authority;
import com.tyson.digitalwallet.ddd.domain.model.entity.Role;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.model.enums.UserStatus;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ToggleUserStatusUseCaseImpl implements ToggleUserStatusUseCase {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TokenStorage tokenStorage;

    public ToggleUserStatusUseCaseImpl(
            UserRepository userRepository,
            WalletRepository walletRepository,
            TokenStorage tokenStorage
    ) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.tokenStorage = tokenStorage;
    }

    @Override
    @Transactional
    public AdminUserSummaryResponse toggleStatus(ToggleUserStatusCommand command) {
        User user = userRepository.findById(command.targetUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + command.targetUserId()));

        if (user.getStatus() == UserStatus.ACTIVE) {
            user.suspend();
            tokenStorage.deleteRefreshToken(user.getId());
            tokenStorage.deleteActiveSession(user.getId());
        } else if (user.getStatus() == UserStatus.SUSPENDED) {
            user.activate();
        }

        User savedUser = userRepository.save(user);

        Set<String> roles = savedUser.getRoles() != null
                ? savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
                : Set.of();

        Set<String> authorities = new HashSet<>();
        if (savedUser.getRoles() != null) {
            for (Role role : savedUser.getRoles()) {
                if (role.getAuthorities() != null) {
                    for (Authority auth : role.getAuthorities()) {
                        authorities.add(auth.getName());
                    }
                }
            }
        }

        Optional<Wallet> walletOpt = walletRepository.findByUserId(savedUser.getId());
        UUID walletId = walletOpt.map(Wallet::getId).orElse(null);
        WalletStatus walletStatus = walletOpt.map(Wallet::getStatus).orElse(null);
        BigDecimal balance = walletOpt.map(Wallet::getBalance).orElse(BigDecimal.ZERO);
        String currency = walletOpt.map(Wallet::getCurrency).orElse("VND");

        return new AdminUserSummaryResponse(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getPhoneNumber(),
                savedUser.getDob(),
                savedUser.getStatus(),
                roles,
                authorities,
                walletId,
                walletStatus,
                balance,
                currency,
                savedUser.getCreatedAt()
        );
    }
}
