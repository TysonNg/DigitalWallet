package com.tyson.digitalwallet.ddd.application.usecase.auth;

import com.tyson.digitalwallet.ddd.application.usecase.auth.command.LoginCommand;
import com.tyson.digitalwallet.ddd.application.usecase.auth.response.AuthResponse;

public interface LoginUseCase {
    AuthResponse login(LoginCommand loginCommand);
}
