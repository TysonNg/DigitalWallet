package com.tyson.digitalwallet.ddd.controller.dto.request.user;

import com.tyson.digitalwallet.ddd.application.usecase.user.command.RegisterUserCommand;

import java.time.LocalDate;

public record RegisterUserRequestDto(
        String fullName,
        String email,
        String phoneNumber,
        LocalDate dob
) {
    public RegisterUserCommand toCommand() {
        return new RegisterUserCommand(
                fullName,
                email,
                phoneNumber,
                dob
        );
    }
}
