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
    private String password;

    private LocalDate dob;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User(
            UUID id,
            String fullName,
            String email,
            String password,
            String phoneNumber,
            LocalDate dob,
            UserStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.dob = dob;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt= updatedAt;
    }

    public static User create(
            String fullName,
            String email,
            String password,
            String phoneNumber,
            LocalDate dob
    ) {
        return new User(
                UUID.randomUUID(),
                fullName,
                email,
                password,
                phoneNumber,
                dob,
                UserStatus.ACTIVE,
                LocalDateTime.now(),
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
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        if (status == UserStatus.CLOSED) {
            throw new IllegalStateException(
                    "Closed user cannot be activated"
            );
        }

        status = UserStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void close() {
        if(status == UserStatus.CLOSED) {
            throw new IllegalStateException(
                    "User is already closed"
            );
        }
        status = UserStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeEmail(String email){
        if(status == UserStatus.CLOSED) {
            throw new IllegalStateException(
                    "User is already closed. You can't change your mail!"
            );
        }
        this.email = email;
        this.updatedAt = LocalDateTime.now();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public LocalDate getDob() {
        return dob;
    }

    public UserStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
