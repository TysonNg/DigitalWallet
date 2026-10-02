package com.tyson.digitalwallet.ddd.controller.dto.response.user;

import com.tyson.digitalwallet.ddd.application.usecase.user.response.UserResponse;
import com.tyson.digitalwallet.ddd.domain.model.enums.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String fullName,
        String email,
        String phoneNumber,
        LocalDate dob,
        UserStatus status,
        LocalDateTime createdAt
) {
    public UserResponseDto(UserResponse r) {
        this(
                r.id(),
                r.fullName(),
                r.email(),
                r.phoneNumber(),
                r.dob(),
                r.status(),
                r.createdAt()
        );
    }

    public static UserResponseDto from(UserResponse response) {
        return response == null ? null : new UserResponseDto(response);
    }
}
