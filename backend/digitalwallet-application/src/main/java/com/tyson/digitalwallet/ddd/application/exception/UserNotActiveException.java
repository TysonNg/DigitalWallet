package com.tyson.digitalwallet.ddd.application.exception;

public class UserNotActiveException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "User account is not active!";

    public UserNotActiveException() {
        super(DEFAULT_MESSAGE);
    }

    public UserNotActiveException(String message) {
        super(message);
    }
}
