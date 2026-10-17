package com.tyson.digitalwallet.ddd.domain.model.entity;

import com.tyson.digitalwallet.ddd.domain.model.enums.UserRole;
import com.tyson.digitalwallet.ddd.domain.model.enums.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class User {

    private UUID id;

    private String fullName;
    private String email;
    private String phoneNumber;
    private String password;

    private Set<Role> roles = new HashSet<>();

    private LocalDate dob;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User(
            UUID id,
            String fullName,
            String email,
            String password,
            Set<Role> roles,
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
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
        this.phoneNumber = phoneNumber;
        this.dob = dob;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt= updatedAt;
    }

    // Overloaded constructor để tương thích chuyển đổi
    public User(
            UUID id,
            String fullName,
            String email,
            String password,
            UserRole role,
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
        this.roles = new HashSet<>();
        if (role != null) {
            this.roles.add(new Role(null, role.name(), null, null));
        } else {
            this.roles.add(new Role(null, UserRole.ROLE_USER.name(), null, null));
        }
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
                new HashSet<>(),
                phoneNumber,
                dob,
                UserStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public Set<Role> getRoles() {
        return roles != null ? roles : new HashSet<>();
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
    }

    public void addRole(Role role) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        if (role != null) {
            this.roles.add(role);
        }
    }

    public void removeRole(String roleName) {
        if (this.roles != null && roleName != null) {
            this.roles.removeIf(r -> roleName.equalsIgnoreCase(r.getName()));
        }
    }

    public boolean hasRole(String roleName) {
        if (this.roles == null || roleName == null) {
            return false;
        }
        return this.roles.stream().anyMatch(r -> roleName.equalsIgnoreCase(r.getName()));
    }

    public boolean hasAuthority(String authorityName) {
        if (this.roles == null || authorityName == null) {
            return false;
        }
        return this.roles.stream().anyMatch(r -> r.hasAuthority(authorityName));
    }

    // Tương thích ngược: lấy role chính đầu tiên (nếu có) hoặc ROLE_USER
    public UserRole getRole() {
        if (roles == null || roles.isEmpty()) {
            return UserRole.ROLE_USER;
        }
        for (Role r : roles) {
            if ("ROLE_ADMIN".equalsIgnoreCase(r.getName())) {
                return UserRole.ROLE_ADMIN;
            }
        }
        return UserRole.ROLE_USER;
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

    public void changePassword(String newHashedPassword) {
        if (status == UserStatus.CLOSED) {
            throw new IllegalStateException(
                    "User is already closed. You can't change your password!"
            );
        }
        if (newHashedPassword == null || newHashedPassword.isBlank()) {
            throw new IllegalArgumentException("Hashed password cannot be blank");
        }
        this.password = newHashedPassword;
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
