package com.tyson.digitalwallet.ddd.domain.model.enums;

public enum PayoutAttemptStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    RETURNED,
    UNKNOWN,
    REVIEW_REQUIRED
}
