package com.tyson.digitalwallet.ddd.application.usecase.admin.command;

import java.util.Set;
import java.util.UUID;

public record AssignUserRolesCommand(
        UUID targetUserId,
        Set<String> roleNames
) {
}
