package com.tyson.digitalwallet.ddd.application.usecase.auth;

import com.tyson.digitalwallet.ddd.application.usecase.auth.command.RegisterCommand;
import com.tyson.digitalwallet.ddd.application.usecase.auth.command.VerifyRegisterOtpCommand;
import com.tyson.digitalwallet.ddd.application.usecase.auth.response.AuthResponse;

public interface RegisterUseCase {
    void register(RegisterCommand registerCommand);
    AuthResponse verifyOtp(VerifyRegisterOtpCommand verifyCommand);
}
