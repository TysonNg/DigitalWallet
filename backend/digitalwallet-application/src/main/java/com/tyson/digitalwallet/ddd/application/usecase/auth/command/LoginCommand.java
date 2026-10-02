package com.tyson.digitalwallet.ddd.application.usecase.auth.command;

public record LoginCommand(
        String email,
        String password
) {}
