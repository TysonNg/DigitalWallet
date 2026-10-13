package com.tyson.digitalwallet.ddd.application.exception;

public class UserNotActiveException extends AppException {
    private static final String DEFAULT_MESSAGE = "User account is not active!";

    public UserNotActiveException() {
        this(DEFAULT_MESSAGE);
    }

    public UserNotActiveException(String message) {
        super(ErrorCode.USER_NOT_ACTIVE, message);
    }
}
