package com.tyson.digitalwallet.ddd.application.usecase.auth.impl;

import com.tyson.digitalwallet.ddd.application.usecase.auth.LoginUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.auth.command.LoginCommand;
import com.tyson.digitalwallet.ddd.application.usecase.auth.response.AuthResponse;
import org.springframework.stereotype.Service;

@Service
public class LoginUseCaseImpl implements LoginUseCase {

    @Override
    public AuthResponse login(LoginCommand loginCommand) {
        return null;
    }
}
