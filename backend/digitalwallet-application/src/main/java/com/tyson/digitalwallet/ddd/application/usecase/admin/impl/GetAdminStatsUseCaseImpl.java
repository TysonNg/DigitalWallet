package com.tyson.digitalwallet.ddd.application.usecase.admin.impl;

import com.tyson.digitalwallet.ddd.application.usecase.admin.GetAdminStatsUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.admin.response.AdminStatsResponse;
import com.tyson.digitalwallet.ddd.domain.model.enums.TransactionStatus;
import com.tyson.digitalwallet.ddd.domain.model.enums.UserStatus;
import com.tyson.digitalwallet.ddd.domain.repository.TransactionRepository;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class GetAdminStatsUseCaseImpl implements GetAdminStatsUseCase {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public GetAdminStatsUseCaseImpl(
            UserRepository userRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository
    ) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponse getSystemStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long suspendedUsers = userRepository.countByStatus(UserStatus.SUSPENDED);

        long totalWallets = walletRepository.count();
        BigDecimal totalWalletBalance = walletRepository.getTotalBalance();

        long totalTransactions = transactionRepository.count();
        BigDecimal totalTransactionVolume = transactionRepository.getTotalVolumeByStatus(TransactionStatus.SUCCESS);

        return new AdminStatsResponse(
                totalUsers,
                activeUsers,
                suspendedUsers,
                totalWallets,
                totalWalletBalance != null ? totalWalletBalance : BigDecimal.ZERO,
                totalTransactions,
                totalTransactionVolume != null ? totalTransactionVolume : BigDecimal.ZERO
        );
    }
}
