package com.tyson.digitalwallet.ddd.controller.dto.request.auth;

import com.tyson.digitalwallet.ddd.application.usecase.auth.command.RegisterCommand;

import java.time.LocalDate;

public record RegisterRequestDto(
        String fullName,
        String email,
        String phoneNumber,
        LocalDate dob,
        String password,
        String confirmPassword
) {
    public RegisterCommand toCommand() {
        return new RegisterCommand(
                fullName,
                email,
                phoneNumber,
                dob,
                password,
                confirmPassword
        );
    }
}
