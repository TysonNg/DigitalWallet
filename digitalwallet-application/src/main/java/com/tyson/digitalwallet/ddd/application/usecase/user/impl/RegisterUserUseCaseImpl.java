package com.tyson.digitalwallet.ddd.application.usecase.user.impl;

import com.tyson.digitalwallet.ddd.application.usecase.user.UserUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.user.command.RegisterUserCommand;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;

public class RegisterUserUseCaseImpl implements UserUseCase {

    @Override
    public User registerAccount(RegisterUserCommand registerUserCommand) {
        return null;
    }

}
