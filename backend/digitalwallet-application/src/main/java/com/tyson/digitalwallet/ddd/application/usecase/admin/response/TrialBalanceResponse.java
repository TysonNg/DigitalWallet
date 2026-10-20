package com.tyson.digitalwallet.ddd.application.usecase.admin.response;

import java.math.BigDecimal;

public record TrialBalanceResponse(
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        boolean balanced,
        BigDecimal difference,
        long totalEntries
) {
}
