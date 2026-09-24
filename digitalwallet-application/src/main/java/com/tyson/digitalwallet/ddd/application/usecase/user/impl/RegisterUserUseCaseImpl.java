package com.tyson.digitalwallet.ddd.application.usecase.user.impl;

import com.tyson.digitalwallet.ddd.application.usecase.user.RegisterUserUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.user.command.RegisterUserCommand;
import com.tyson.digitalwallet.ddd.application.usecase.user.response.UserResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private final UserRepository userRepository;

    public RegisterUserUseCaseImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserResponse registerAccount(RegisterUserCommand command) {
        if (userRepository.existByEmail(command.email())) {
            throw new IllegalArgumentException("Email already exists: " + command.email());
        }

        User user = User.create(
                command.fullName(),
                command.email(),
                command.phoneNumber(),
                command.dob()
        );

        if (!user.canCreateWallet()) {
            throw new IllegalArgumentException("User must be at least 18 years old to create a wallet");
        }

        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getPhoneNumber(),
                savedUser.getDob(),
                savedUser.getStatus(),
                savedUser.getCreatedAt()
        );
    }
}
