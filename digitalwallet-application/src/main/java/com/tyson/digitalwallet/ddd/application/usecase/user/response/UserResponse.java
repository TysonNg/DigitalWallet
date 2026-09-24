package com.tyson.digitalwallet.ddd.application.usecase.user.response;

import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.model.enums.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String phoneNumber,
        LocalDate dob,
        UserStatus status,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        if(user == null) return null;

        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getDob(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
