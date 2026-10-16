package com.tyson.digitalwallet.ddd.application.usecase.wallet.pin;

import com.tyson.digitalwallet.ddd.application.exception.AppException;
import com.tyson.digitalwallet.ddd.application.exception.ErrorCode;

import java.util.Set;

public final class WalletPinValidator {

    private static final Set<String> WEAK_PINS = Set.of(
            "000000", "111111", "222222", "333333", "444444",
            "555555", "666666", "777777", "888888", "999999",
            "123456", "654321", "012345", "543210"
    );

    private WalletPinValidator() {}

    public static void validatePin(String pin) {
        if (pin == null || !pin.matches("^[0-9]{6}$")) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Mã PIN phải bao gồm đúng 6 chữ số");
        }

        if (WEAK_PINS.contains(pin)) {
            throw new AppException(ErrorCode.WEAK_WALLET_PIN, "Mã PIN quá đơn giản hoặc dễ đoán. Vui lòng chọn mã PIN khác");
        }
    }
}
