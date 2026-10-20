package com.tyson.digitalwallet.ddd.domain.exception;

public class UnbalancedLedgerException extends RuntimeException {
    public UnbalancedLedgerException(String message) {
        super(message);
    }
}
