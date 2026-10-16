package com.tyson.digitalwallet.ddd.controller.dto.request.user;

import com.tyson.digitalwallet.ddd.application.usecase.user.command.ChangePasswordCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChangePasswordRequestDto(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "New password must be at least 6 characters")
        String newPassword
) {
    public ChangePasswordCommand toCommand(UUID userId) {
        return new ChangePasswordCommand(userId, currentPassword, newPassword);
    }
}
