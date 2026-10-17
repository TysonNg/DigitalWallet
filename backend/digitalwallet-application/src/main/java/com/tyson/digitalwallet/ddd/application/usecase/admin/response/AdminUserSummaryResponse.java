package com.tyson.digitalwallet.ddd.application.usecase.admin.response;

import com.tyson.digitalwallet.ddd.domain.model.enums.UserStatus;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record AdminUserSummaryResponse(
        UUID id,
        String fullName,
        String email,
        String phoneNumber,
        LocalDate dob,
        UserStatus status,
        Set<String> roles,
        Set<String> authorities,
        UUID walletId,
        WalletStatus walletStatus,
        BigDecimal walletBalance,
        String walletCurrency,
        LocalDateTime createdAt
) {
}

