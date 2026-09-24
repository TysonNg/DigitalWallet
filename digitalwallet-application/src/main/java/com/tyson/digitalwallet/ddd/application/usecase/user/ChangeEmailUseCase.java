package com.tyson.digitalwallet.ddd.application.usecase.user;

import com.tyson.digitalwallet.ddd.application.usecase.user.command.ChangeEmailCommand;
import com.tyson.digitalwallet.ddd.application.usecase.user.response.UserResponse;

public interface ChangeEmailUseCase {
    UserResponse changeEmail(ChangeEmailCommand command);
}
