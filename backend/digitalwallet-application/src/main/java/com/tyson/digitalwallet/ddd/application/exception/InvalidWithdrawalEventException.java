package com.tyson.digitalwallet.ddd.application.exception;

/** A malformed or incompatible event must be quarantined, not retried as a provider failure. */
public class InvalidWithdrawalEventException extends RuntimeException {
    public InvalidWithdrawalEventException(String message) {
        super(message);
    }
}
