package com.tyson.digitalwallet.ddd.application.exception;

public class WalletNotFoundException extends RuntimeException{

    public WalletNotFoundException(String message){
        super(message);
    }
}
