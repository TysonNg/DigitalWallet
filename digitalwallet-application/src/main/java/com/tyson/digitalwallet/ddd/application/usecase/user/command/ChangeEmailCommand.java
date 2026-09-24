package com.tyson.digitalwallet.ddd.application.usecase.user.command;

import java.util.UUID;

public record ChangeEmailCommand(
        UUID userId,
        String newEmail
) {}
