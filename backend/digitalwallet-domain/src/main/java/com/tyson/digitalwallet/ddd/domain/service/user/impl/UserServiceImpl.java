package com.tyson.digitalwallet.ddd.domain.service.user.impl;

import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.domain.service.user.UserService;

import java.time.LocalDate;
import java.util.UUID;

public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User registerUser(String fullName, String email, String password, String phoneNumber, LocalDate dob) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (userRepository.existByEmail(email)) {
            throw new IllegalStateException("Email already registered: " + email);
        }

        User user = User.create(fullName, email, password, phoneNumber, dob);
        return userRepository.save(user);
    }

    @Override
    public void changePassword(UUID userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setPassword(newPassword);
        userRepository.save(user);
    }

    @Override
    public void suspendUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.suspend();
        userRepository.save(user);
    }

    @Override
    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.activate();
        userRepository.save(user);
    }
}
