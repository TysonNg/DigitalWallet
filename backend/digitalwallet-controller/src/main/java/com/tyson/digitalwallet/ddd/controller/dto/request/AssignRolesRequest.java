package com.tyson.digitalwallet.ddd.controller.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record AssignRolesRequest(
        @NotEmpty(message = "Roles set cannot be empty")
        Set<String> roles
) {
}
