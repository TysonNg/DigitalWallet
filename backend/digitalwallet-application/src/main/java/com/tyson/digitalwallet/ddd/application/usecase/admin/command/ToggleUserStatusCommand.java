package com.tyson.digitalwallet.ddd.application.usecase.admin.command;

import java.util.UUID;

public record ToggleUserStatusCommand(
        UUID targetUserId
) {
}
