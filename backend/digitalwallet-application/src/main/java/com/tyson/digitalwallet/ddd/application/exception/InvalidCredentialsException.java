package com.tyson.digitalwallet.ddd.application.exception;

public class InvalidCredentialsException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Invalid email or password!";

    public InvalidCredentialsException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
