package com.tyson.digitalwallet.ddd.application.usecase.auth.impl;

import com.tyson.digitalwallet.ddd.application.exception.ExistEmailException;
import com.tyson.digitalwallet.ddd.application.usecase.auth.RegisterUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.auth.command.RegisterCommand;
import com.tyson.digitalwallet.ddd.application.usecase.auth.response.AuthResponse;
import com.tyson.digitalwallet.ddd.domain.event.UserRegisteredEvent;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.domain.service.security.PasswordHasher;
import com.tyson.digitalwallet.ddd.domain.service.security.TokenProvider;
import com.tyson.digitalwallet.ddd.domain.service.security.TokenStorage;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

public class RegisterUseCaseImpl implements RegisterUseCase {
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordHasher passwordHasher;
    private final TokenStorage tokenStorage;
    private final TokenProvider tokenProvider;


    public RegisterUseCaseImpl(UserRepository userRepository, ApplicationEventPublisher eventPublisher, PasswordHasher passwordHasher, TokenStorage tokenStorage, TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.passwordHasher = passwordHasher;
        this.tokenStorage = tokenStorage;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public AuthResponse register(RegisterCommand registerCommand) {
        Boolean isExistEmail = userRepository.existByEmail(registerCommand.email());
        String password = registerCommand.password();
        String confirmPassword = registerCommand.confirmPassword();

        if(isExistEmail) throw new ExistEmailException();
        if(!password.equals(confirmPassword)) throw new RuntimeException("Not match password");

        String hashedPassword = passwordHasher.hash(password);

        User user = User.create(
                registerCommand.fullName(),
                registerCommand.email(),
                hashedPassword,
                registerCommand.phoneNumber(),
                registerCommand.dob()
        );

        if(!user.canCreateWallet()) {
            throw new IllegalArgumentException("User must be at least 18 years old to create a wallet");
        }

        User savedUser = userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(savedUser);
        String refreshToken = tokenProvider.generateRefreshToken(savedUser);

        tokenStorage.saveRefreshToken(refreshToken, savedUser.getId());

        eventPublisher.publishEvent(new UserRegisteredEvent(
                savedUser.getId(),
                savedUser.getEmail(),
                Instant.now()
        ));

        return new AuthResponse(
                accessToken,
                refreshToken,
                savedUser
        );
    }
}
