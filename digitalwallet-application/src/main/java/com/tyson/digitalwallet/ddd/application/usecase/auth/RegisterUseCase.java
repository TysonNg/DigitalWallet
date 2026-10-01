package com.tyson.digitalwallet.ddd.application.usecase.auth;

import com.tyson.digitalwallet.ddd.application.usecase.auth.command.RegisterCommand;
import com.tyson.digitalwallet.ddd.application.usecase.auth.response.AuthResponse;

public interface RegisterUseCase {
    AuthResponse register(RegisterCommand registerCommand);
}
