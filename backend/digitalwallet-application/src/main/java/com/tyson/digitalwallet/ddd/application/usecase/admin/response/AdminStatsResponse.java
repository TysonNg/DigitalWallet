package com.tyson.digitalwallet.ddd.application.usecase.admin.response;

import java.math.BigDecimal;

public record AdminStatsResponse(
        long totalUsers,
        long activeUsers,
        long suspendedUsers,
        long totalWallets,
        BigDecimal totalWalletBalance,
        long totalTransactions,
        BigDecimal totalTransactionVolume
) {
}
