package com.tyson.digitalwallet.ddd.application.usecase.user.command;

import java.time.LocalDate;

public record RegisterUserCommand(
        String fullName,
        String email,
        String phoneNumber,
        LocalDate dob
) {}
