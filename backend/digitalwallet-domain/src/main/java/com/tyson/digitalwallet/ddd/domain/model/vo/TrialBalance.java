package com.tyson.digitalwallet.ddd.domain.model.vo;

import java.math.BigDecimal;

public record TrialBalance(
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        boolean balanced,
        BigDecimal difference,
        long totalEntries
) {
    public static TrialBalance of(BigDecimal totalDebit, BigDecimal totalCredit, long totalEntries) {
        BigDecimal debit = (totalDebit != null) ? totalDebit : BigDecimal.ZERO;
        BigDecimal credit = (totalCredit != null) ? totalCredit : BigDecimal.ZERO;
        BigDecimal diff = debit.subtract(credit).abs();
        boolean isBalanced = diff.compareTo(BigDecimal.ZERO) == 0;
        return new TrialBalance(debit, credit, isBalanced, diff, totalEntries);
    }
}
