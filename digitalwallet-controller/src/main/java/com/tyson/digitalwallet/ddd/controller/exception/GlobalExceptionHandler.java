package com.tyson.digitalwallet.ddd.controller.exception;

import com.tyson.digitalwallet.ddd.application.exception.WalletNotFoundException;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.enums.ErrorStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WalletNotFoundException.class)
    public ApiResponse<Void> handleWalletNotFound(WalletNotFoundException ex) {
        return ApiResponse.error(
                ErrorStatus.NOTFOUND.getCode(),
                ex.getMessage()
        );
    }

}
