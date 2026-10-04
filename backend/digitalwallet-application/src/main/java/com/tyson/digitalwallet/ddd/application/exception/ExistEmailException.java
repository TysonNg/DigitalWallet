package com.tyson.digitalwallet.ddd.application.exception;

public class ExistEmailException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Email already exists!";

    public ExistEmailException() {
        super(DEFAULT_MESSAGE);
    }

    public ExistEmailException(String message) {
        super(message);
    }
}
