package com.tyson.digitalwallet.ddd.application.usecase.user.command;

import java.util.UUID;

public record ChangePasswordCommand(
        UUID userId,
        String currentPassword,
        String newPassword
) {}
