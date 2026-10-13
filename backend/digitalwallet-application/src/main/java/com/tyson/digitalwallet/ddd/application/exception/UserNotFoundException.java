package com.tyson.digitalwallet.ddd.application.exception;

public class UserNotFoundException extends AppException {
    public UserNotFoundException(String message) {
        super(ErrorCode.USER_NOT_FOUND, message);
    }
}
