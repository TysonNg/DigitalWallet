package com.tyson.digitalwallet.ddd.application.usecase.auth.command;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;

public record PendingRegistration(
        String fullName,
        String email,
        String phoneNumber,
        String dob,
        String hashedPassword
) {
    @JsonIgnore
    public LocalDate getLocalDateDob() {
        return dob != null ? LocalDate.parse(dob) : null;
    }
}
