package com.tyson.digitalwallet.ddd.application.exception;

public class InvalidOtpException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Invalid or expired OTP!";

    public InvalidOtpException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidOtpException(String message) {
        super(message);
    }
}
