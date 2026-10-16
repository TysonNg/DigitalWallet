package com.tyson.digitalwallet.ddd.controller.dto.request.wallet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record ChangePinRequestDto(
        UUID walletId,

        @NotBlank(message = "Mã PIN hiện tại là bắt buộc")
        @Pattern(regexp = "^[0-9]{6}$", message = "Mã PIN phải bao gồm đúng 6 chữ số")
        String oldPin,

        @NotBlank(message = "Mã PIN mới là bắt buộc")
        @Pattern(regexp = "^[0-9]{6}$", message = "Mã PIN phải bao gồm đúng 6 chữ số")
        String newPin
) {}
