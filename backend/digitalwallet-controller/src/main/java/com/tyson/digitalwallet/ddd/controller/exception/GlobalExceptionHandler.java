package com.tyson.digitalwallet.ddd.controller.exception;

import com.tyson.digitalwallet.ddd.application.exception.AppException;
import com.tyson.digitalwallet.ddd.application.exception.ErrorCode;
import com.tyson.digitalwallet.ddd.application.exception.ExistEmailException;
import com.tyson.digitalwallet.ddd.application.exception.InvalidCredentialsException;
import com.tyson.digitalwallet.ddd.application.exception.InvalidOtpException;
import com.tyson.digitalwallet.ddd.application.exception.SessionExpiredException;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.enums.ErrorStatus;
import com.tyson.digitalwallet.ddd.domain.exception.DistributedLockException;
import com.tyson.digitalwallet.ddd.domain.exception.PaymentGatewayUnavailableException;
import com.tyson.digitalwallet.ddd.domain.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
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
            case TOO_MANY_REQUESTS -> ErrorStatus.TOO_MANY_REQUESTS;
            case PAYMENT_GATEWAY_UNAVAILABLE, SERVICE_UNAVAILABLE -> ErrorStatus.SERVICE_UNAVAILABLE;
            default -> ErrorStatus.BAD_REQUEST;
        };
        return ApiResponse.error(status, ex.getMessage(), ex.getErrorCode().getCode());
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimit(RequestNotPermitted ex) {
        return ApiResponse.error(
                ErrorStatus.TOO_MANY_REQUESTS,
                "Bạn đang gửi yêu cầu quá nhanh. Vui lòng thử lại sau.",
                ErrorCode.TOO_MANY_REQUESTS.getCode()
        );
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCircuitBreaker(CallNotPermittedException ex) {
        return ApiResponse.error(
                ErrorStatus.SERVICE_UNAVAILABLE,
                "Dịch vụ tạm thời gián đoạn do hệ thống bảo vệ ngắt mạch. Vui lòng thử lại sau ít phút.",
                ErrorCode.SERVICE_UNAVAILABLE.getCode()
        );
    }

    @ExceptionHandler(PaymentGatewayUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentGatewayUnavailable(PaymentGatewayUnavailableException ex) {
        return ApiResponse.error(
                ErrorStatus.SERVICE_UNAVAILABLE,
                ex.getMessage(),
                ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE.getCode()
        );
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceUnavailable(ServiceUnavailableException ex) {
        return ApiResponse.error(
                ErrorStatus.SERVICE_UNAVAILABLE,
                ex.getMessage(),
                ErrorCode.SERVICE_UNAVAILABLE.getCode()
        );
    }

    @ExceptionHandler(DistributedLockException.class)
    public ResponseEntity<ApiResponse<Void>> handleDistributedLockException(DistributedLockException ex) {
        return ApiResponse.error(
                ErrorStatus.TOO_MANY_REQUESTS,
                ex.getMessage(),
                "TRANSACTION_IN_PROGRESS"
        );
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
