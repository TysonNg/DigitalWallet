package com.tyson.digitalwallet.ddd.application.usecase.user;

import com.tyson.digitalwallet.ddd.application.usecase.user.command.RegisterUserCommand;
import com.tyson.digitalwallet.ddd.application.usecase.user.response.UserResponse;

public interface RegisterUserUseCase {
    UserResponse registerAccount(RegisterUserCommand command);
}
