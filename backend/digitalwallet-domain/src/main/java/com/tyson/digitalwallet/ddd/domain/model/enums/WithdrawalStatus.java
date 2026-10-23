package com.tyson.digitalwallet.ddd.domain.model.enums;

public enum WithdrawalStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    RETURNED,
    REVIEW_REQUIRED
}
