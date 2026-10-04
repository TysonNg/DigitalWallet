package com.tyson.digitalwallet.ddd.application.usecase.auth.command;

public record RefreshTokenCommand(
        String refreshToken
) {
}
