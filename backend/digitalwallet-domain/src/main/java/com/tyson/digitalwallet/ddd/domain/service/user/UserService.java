package com.tyson.digitalwallet.ddd.domain.service.user;

import com.tyson.digitalwallet.ddd.domain.model.entity.User;

import java.time.LocalDate;
import java.util.UUID;

public interface UserService {
    User registerUser(String fullName, String email, String password, String phoneNumber, LocalDate dob);

    void changePassword(UUID userId, String newPassword);

    void suspendUser(UUID userId);

    void activateUser(UUID userId);
}
