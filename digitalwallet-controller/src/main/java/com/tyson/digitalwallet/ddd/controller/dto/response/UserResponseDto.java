package com.tyson.digitalwallet.ddd.controller.dto.response;

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
    public static UserResponseDto from(UserResponse response) {
        if (response == null) {
            return null;
        }

        return new UserResponseDto(
                response.id(),
                response.fullName(),
                response.email(),
                response.phoneNumber(),
                response.dob(),
                response.status(),
                response.createdAt()
        );
    }
}
