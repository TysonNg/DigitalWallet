package com.tyson.digitalwallet.ddd.controller.dto.request;

import com.tyson.digitalwallet.ddd.application.usecase.user.command.ChangeEmailCommand;

import java.util.UUID;

public record ChangeEmailUserRequestDto(
        UUID id,
        String newEmail
) {
    public ChangeEmailCommand toCommand() {
        return new ChangeEmailCommand(
                id,
                newEmail
        );
    }
}
