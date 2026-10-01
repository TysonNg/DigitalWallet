package com.tyson.digitalwallet.ddd.application.usecase.auth.command;

import java.time.LocalDate;

public record RegisterCommand(
        String fullName,
        String email,
        String phoneNumber,
        LocalDate dob,
        String password,
        String confirmPassword
) {}
