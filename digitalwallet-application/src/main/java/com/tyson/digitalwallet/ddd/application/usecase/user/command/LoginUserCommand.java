package com.tyson.digitalwallet.ddd.application.usecase.user.command;

public record LoginUserCommand(
        String email,
        String password
) {}
