package com.tyson.digitalwallet.ddd.application.usecase.wallet.pin;

import com.tyson.digitalwallet.ddd.application.exception.AppException;
import com.tyson.digitalwallet.ddd.application.exception.ErrorCode;

public final class WalletPinValidator {

    private WalletPinValidator() {}

    public static void validatePin(String pin) {
        if (pin == null || !pin.matches("^[0-9]{6}$")) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Mã PIN phải bao gồm đúng 6 chữ số");
        }
    }
}
