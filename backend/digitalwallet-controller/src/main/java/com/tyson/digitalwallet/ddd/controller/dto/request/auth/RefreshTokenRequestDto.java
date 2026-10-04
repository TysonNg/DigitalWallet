package com.tyson.digitalwallet.ddd.controller.dto.request.auth;

import com.tyson.digitalwallet.ddd.application.usecase.auth.command.RefreshTokenCommand;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDto(
        @NotBlank(message = "Refresh token must not be blank")
        String refreshToken
) {
    public RefreshTokenCommand toCommand() {
        return new RefreshTokenCommand(refreshToken);
    }
}
