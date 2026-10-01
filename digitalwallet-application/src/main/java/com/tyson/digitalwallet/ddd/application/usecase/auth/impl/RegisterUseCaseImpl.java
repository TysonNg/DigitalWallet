package com.tyson.digitalwallet.ddd.application.usecase.auth.impl;

import com.tyson.digitalwallet.ddd.application.exception.ExistEmailException;
import com.tyson.digitalwallet.ddd.application.usecase.auth.RegisterUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.auth.command.RegisterCommand;
import com.tyson.digitalwallet.ddd.application.usecase.auth.response.AuthResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;

public class RegisterUseCaseImpl implements RegisterUseCase {
    private final UserRepository userRepository;

    public RegisterUseCaseImpl(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    public AuthResponse register(RegisterCommand registerCommand) {
        Boolean isExistEmail = userRepository.existByEmail(registerCommand.email());

        if(isExistEmail) throw new ExistEmailException();

        User user = User.create(
                registerCommand.fullName(),
                registerCommand.email(),
                registerCommand.password(),
                registerCommand.phoneNumber(),
                registerCommand.dob()
        );

        if(!user.canCreateWallet()) {
            throw new IllegalArgumentException("User must be at least 18 years old to create a wallet");
        }

        User savedUser = userRepository.save(user);

        return null;
    }
}
