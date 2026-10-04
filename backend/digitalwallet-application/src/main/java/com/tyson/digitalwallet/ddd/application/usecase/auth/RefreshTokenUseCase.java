package com.tyson.digitalwallet.ddd.application.usecase.auth;

import com.tyson.digitalwallet.ddd.application.usecase.auth.command.RefreshTokenCommand;
import com.tyson.digitalwallet.ddd.application.usecase.auth.response.AuthResponse;

public interface RefreshTokenUseCase {
    AuthResponse refreshToken(RefreshTokenCommand command);
}
