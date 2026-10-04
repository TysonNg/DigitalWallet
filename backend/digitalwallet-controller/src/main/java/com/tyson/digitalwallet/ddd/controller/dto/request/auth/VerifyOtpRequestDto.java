package com.tyson.digitalwallet.ddd.controller.dto.request.auth;

import com.tyson.digitalwallet.ddd.application.usecase.auth.command.VerifyRegisterOtpCommand;

public record VerifyOtpRequestDto(
        String email,
        String otp
) {
    public VerifyRegisterOtpCommand toCommand() {
        return new VerifyRegisterOtpCommand(email, otp);
    }
}
