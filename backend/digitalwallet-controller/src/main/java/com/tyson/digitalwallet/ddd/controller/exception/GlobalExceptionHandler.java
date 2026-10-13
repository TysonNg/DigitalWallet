package com.tyson.digitalwallet.ddd.controller.exception;

import com.tyson.digitalwallet.ddd.application.exception.AppException;
import com.tyson.digitalwallet.ddd.application.exception.ExistEmailException;
import com.tyson.digitalwallet.ddd.application.exception.InvalidCredentialsException;
import com.tyson.digitalwallet.ddd.application.exception.InvalidOtpException;
import com.tyson.digitalwallet.ddd.application.exception.SessionExpiredException;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.enums.ErrorStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        ErrorStatus status = switch (ex.getErrorCode()) {
            case USER_NOT_FOUND, WALLET_NOT_FOUND, BENEFICIARY_NOT_FOUND -> ErrorStatus.NOTFOUND;
            case FORBIDDEN_ACCESS -> ErrorStatus.FORBIDDEN;
            case UNAUTHORIZED, SESSION_EXPIRED, INVALID_CREDENTIALS -> ErrorStatus.UNAUTHORIZED;
            default -> ErrorStatus.BAD_REQUEST;
        };
        return ApiResponse.error(status, ex.getMessage(), ex.getErrorCode().getCode());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(SecurityException ex) {
        return ApiResponse.error(ErrorStatus.FORBIDDEN, ex.getMessage(), "FORBIDDEN_ACCESS");
    }

    @ExceptionHandler(ExistEmailException.class)
    public ResponseEntity<ApiResponse<Void>> handleExistEmail(ExistEmailException ex) {
        return ApiResponse.error(
                ErrorStatus.BAD_REQUEST,
                ex.getMessage(),
                "EMAIL_ALREADY_EXISTS"
        );
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOtp(InvalidOtpException ex) {
        return ApiResponse.error(
                ErrorStatus.BAD_REQUEST,
                ex.getMessage(),
                "INVALID_OTP"
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ApiResponse.error(
                ErrorStatus.UNAUTHORIZED,
                ex.getMessage(),
                "INVALID_CREDENTIALS"
        );
    }

    @ExceptionHandler(SessionExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleSessionExpired(SessionExpiredException ex) {
        return ApiResponse.error(
                ErrorStatus.UNAUTHORIZED,
                ex.getMessage(),
                "SESSION_EXPIRED"
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.error(
                ErrorStatus.BAD_REQUEST,
                ex.getMessage(),
                "INVALID_ARGUMENT"
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        return ApiResponse.error(
                ErrorStatus.BAD_REQUEST,
                ex.getMessage(),
                "ILLEGAL_STATE"
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
                errorMessage,
                "VALIDATION_ERROR"
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        return ApiResponse.error(
                ErrorStatus.INTERNAL_ERROR,
                ex.getMessage() != null && !ex.getMessage().isBlank()
                        ? ex.getMessage()
                        : "An unexpected internal server error occurred.",
                "INTERNAL_SERVER_ERROR"
        );
    }
}
