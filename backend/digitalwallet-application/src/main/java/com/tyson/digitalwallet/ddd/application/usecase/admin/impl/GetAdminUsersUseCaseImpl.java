package com.tyson.digitalwallet.ddd.application.usecase.admin.impl;

import com.tyson.digitalwallet.ddd.application.usecase.admin.GetAdminUsersUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.admin.response.AdminUserSummaryResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.Authority;
import com.tyson.digitalwallet.ddd.domain.model.entity.Role;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GetAdminUsersUseCaseImpl implements GetAdminUsersUseCase {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public GetAdminUsersUseCaseImpl(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserSummaryResponse> getAllUsers() {
        List<User> users = userRepository.findAll();

        return users.stream().map(user -> {
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

            Optional<Wallet> walletOpt = walletRepository.findByUserId(user.getId());
            UUID walletId = walletOpt.map(Wallet::getId).orElse(null);
            WalletStatus walletStatus = walletOpt.map(Wallet::getStatus).orElse(null);
            BigDecimal balance = walletOpt.map(Wallet::getBalance).orElse(BigDecimal.ZERO);
            String currency = walletOpt.map(Wallet::getCurrency).orElse("VND");

            return new AdminUserSummaryResponse(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhoneNumber(),
                    user.getDob(),
                    user.getStatus(),
                    roles,
                    authorities,
                    walletId,
                    walletStatus,
                    balance,
                    currency,
                    user.getCreatedAt()
            );
        }).toList();
    }
}
