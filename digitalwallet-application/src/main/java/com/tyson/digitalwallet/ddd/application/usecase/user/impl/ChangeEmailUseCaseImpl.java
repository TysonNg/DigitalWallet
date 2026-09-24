package com.tyson.digitalwallet.ddd.application.usecase.user.impl;

import com.tyson.digitalwallet.ddd.application.usecase.user.ChangeEmailUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.user.command.ChangeEmailCommand;
import com.tyson.digitalwallet.ddd.application.usecase.user.response.UserResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class ChangeEmailUseCaseImpl implements ChangeEmailUseCase {

    private final UserRepository userRepository;

    public ChangeEmailUseCaseImpl (UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserResponse changeEmail(ChangeEmailCommand command) {
        User user = userRepository.findById(command.userId()).orElseThrow(() -> new NoSuchElementException("User not found with id: " + command.userId()));

        if(user.getEmail().equalsIgnoreCase(command.newEmail())) {
            throw new IllegalArgumentException("New email must be different from current email");
        }

        if(userRepository.existByEmail(command.newEmail())) {
            throw new IllegalArgumentException("Email already in use: " + command.newEmail());
        }

        user.changeEmail(command.newEmail());

        User savedUser = userRepository.save(user);

        return UserResponse.from(savedUser);

    }
}
