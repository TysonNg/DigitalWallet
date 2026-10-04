package com.tyson.digitalwallet.ddd.application.usecase.auth;

public interface LogoutUseCase {
    void logout(String accessToken);
}
