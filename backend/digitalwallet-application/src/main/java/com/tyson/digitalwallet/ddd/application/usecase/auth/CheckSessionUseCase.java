package com.tyson.digitalwallet.ddd.application.usecase.auth;

public interface CheckSessionUseCase {
    void checkSession(String token);
}
