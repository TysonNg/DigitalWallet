package com.tyson.digitalwallet.ddd.application.usecase.user.impl;

import com.tyson.digitalwallet.ddd.application.usecase.user.GetUserUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.user.response.UserResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class GetUserUseCaseImpl implements GetUserUseCase {

    private final UserRepository userRepository;

    public GetUserUseCaseImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + id));

        return UserResponse.from(user);
    }
}
