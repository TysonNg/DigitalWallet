package com.tyson.digitalwallet.ddd.controller.dto.request.auth;

import com.tyson.digitalwallet.ddd.application.usecase.auth.command.LoginCommand;
import jakarta.validation.constraints.NotNull;

public record LoginDto(
        @NotNull String email,
        @NotNull String password
) {
    public LoginCommand toCommand() {
        return new LoginCommand(
                email,
                password
        );
    }
}
