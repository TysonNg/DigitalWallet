package com.tyson.digitalwallet.ddd.application.usecase.auth.command;

public record VerifyRegisterOtpCommand(
        String email,
        String otp
) {
}
