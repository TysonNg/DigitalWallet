package com.tyson.digitalwallet.ddd.application.usecase.auth.command;

import java.time.LocalDate;

public record PendingRegistration(
        String fullName,
        String email,
        String phoneNumber,
        String dob,
        String hashedPassword
) {
    public LocalDate getLocalDateDob() {
        return dob != null ? LocalDate.parse(dob) : null;
    }
}
