package com.tyson.digitalwallet.ddd.application.usecase.user;

import com.tyson.digitalwallet.ddd.application.usecase.user.command.ChangePasswordCommand;

public interface ChangePasswordUseCase {
    void changePassword(ChangePasswordCommand command);
}
