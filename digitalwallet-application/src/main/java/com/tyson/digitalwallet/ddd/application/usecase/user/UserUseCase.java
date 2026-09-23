package com.tyson.digitalwallet.ddd.application.usecase.user;

import com.tyson.digitalwallet.ddd.application.usecase.user.command.RegisterUserCommand;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;

public interface UserUseCase {
    User registerAccount(RegisterUserCommand registerUserCommand);
}
