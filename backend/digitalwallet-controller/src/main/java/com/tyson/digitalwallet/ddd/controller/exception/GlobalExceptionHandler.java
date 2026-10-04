package com.tyson.digitalwallet.ddd.controller.exception;

import com.tyson.digitalwallet.ddd.application.exception.ExistEmailException;
import com.tyson.digitalwallet.ddd.application.exception.InvalidCredentialsException;
import com.tyson.digitalwallet.ddd.application.exception.InvalidOtpException;
import com.tyson.digitalwallet.ddd.application.exception.SessionExpiredException;
import com.tyson.digitalwallet.ddd.application.exception.UserNotActiveException;
import com.tyson.digitalwallet.ddd.application.exception.WalletNotFoundException;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.enums.ErrorStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleWalletNotFound(WalletNotFoundException ex) {
        return ApiResponse.error(
                ErrorStatus.NOTFOUND,
                ex.getMessage()
        );
    }

    @ExceptionHandler(ExistEmailException.class)
    public ResponseEntity<ApiResponse<Void>> handleExistEmail(ExistEmailException ex) {
        return ApiResponse.error(
                ErrorStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOtp(InvalidOtpException ex) {
        return ApiResponse.error(
                ErrorStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ApiResponse.error(
                ErrorStatus.UNAUTHORIZED,
                ex.getMessage()
        );
    }

    @ExceptionHandler(UserNotActiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotActive(UserNotActiveException ex) {
        return ApiResponse.error(
                ErrorStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    @ExceptionHandler(SessionExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleSessionExpired(SessionExpiredException ex) {
        return ApiResponse.error(
                ErrorStatus.UNAUTHORIZED,
                ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.error(
                ErrorStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        return ApiResponse.error(
                ErrorStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");

        return ApiResponse.error(
                ErrorStatus.BAD_REQUEST,
                errorMessage
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        return ApiResponse.error(
                ErrorStatus.INTERNAL_ERROR,
                ex.getMessage() != null && !ex.getMessage().isBlank()
                        ? ex.getMessage()
                        : "An unexpected internal server error occurred."
        );
    }
}
