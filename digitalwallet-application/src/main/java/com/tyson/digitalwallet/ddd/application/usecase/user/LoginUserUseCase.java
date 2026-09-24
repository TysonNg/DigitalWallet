package com.tyson.digitalwallet.ddd.application.usecase.user;

import com.tyson.digitalwallet.ddd.application.usecase.user.command.LoginUserCommand;
import com.tyson.digitalwallet.ddd.application.usecase.user.response.UserResponse;

public interface LoginUserUseCase {
    UserResponse login(LoginUserCommand command);
}
