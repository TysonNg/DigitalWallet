package com.tyson.digitalwallet.ddd.application.usecase.admin.response;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletReconciliationResponse(
        UUID walletId,
        BigDecimal walletBalance,
        boolean reconciled
) {
}
