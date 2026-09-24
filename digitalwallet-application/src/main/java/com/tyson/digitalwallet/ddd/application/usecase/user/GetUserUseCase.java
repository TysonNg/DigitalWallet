package com.tyson.digitalwallet.ddd.application.usecase.user;

import com.tyson.digitalwallet.ddd.application.usecase.user.response.UserResponse;

import java.util.UUID;

public interface GetUserUseCase {
    UserResponse getUserById(UUID id);
}
