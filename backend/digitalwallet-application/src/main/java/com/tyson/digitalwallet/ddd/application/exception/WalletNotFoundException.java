package com.tyson.digitalwallet.ddd.application.exception;

public class WalletNotFoundException extends AppException {

    public WalletNotFoundException(String message) {
        super(ErrorCode.WALLET_NOT_FOUND, message);
    }
}
