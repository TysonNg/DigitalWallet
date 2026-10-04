package com.tyson.digitalwallet.ddd.application.exception;

public class SessionExpiredException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Your account has been logged in on another device.";

    public SessionExpiredException() {
        super(DEFAULT_MESSAGE);
    }

    public SessionExpiredException(String message) {
        super(message);
    }
}
