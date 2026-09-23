package com.tyson.digitalwallet.ddd.domain.model.entity;

import com.tyson.digitalwallet.ddd.domain.model.enums.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

public class User {

    private UUID id;

    private String fullName;
    private String email;
    private String phoneNumber;

    private LocalDate dob;
    private UserStatus status;
    private LocalDateTime createdAt;

    public User(UUID id, String fullName, String email, String phoneNumber, LocalDate dob, UserStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.dob = dob;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static User create(
            String fullName,
            String email,
            String phoneNumber,
            LocalDate dob
    ) {
        return new User(
                UUID.randomUUID(),
                fullName,
                email,
                phoneNumber,
                dob,
                UserStatus.ACTIVE,
                LocalDateTime.now()
        );
    }

    public boolean canCreateWallet() {
        return Period.between(dob, LocalDate.now()).getYears() >= 18;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public void suspend() {
        if(status == UserStatus.CLOSED) {
            throw new IllegalStateException(
                    "Closed user cannot be suspended"
            );
        }
        status = UserStatus.SUSPENDED;
    }

    public void activate() {
        if (status == UserStatus.CLOSED) {
            throw new IllegalStateException(
                    "Closed user cannot be activated"
            );
        }

        status = UserStatus.ACTIVE;
    }

    public void close() {
        if(status == UserStatus.CLOSED) {
            throw new IllegalStateException(
                    "User is already closed"
            );
        }
        status = UserStatus.CLOSED;
    }

    public void changeEmail(String email){
        if(status == UserStatus.CLOSED) {
            throw new IllegalStateException(
                    "User is already closed. You can't change your mail!"
            );
        }
        this.email = email;
    }

}
