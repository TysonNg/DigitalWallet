package com.tyson.digitalwallet.ddd.controller.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tyson.digitalwallet.ddd.controller.enums.ErrorStatus;
import com.tyson.digitalwallet.ddd.controller.enums.SuccessStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int statusCode,
        String message,
        T data,
        Instant timestamp
) {

    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, SuccessStatus.SUCCESS.getCode(), message, data, Instant.now())
        );
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(true, SuccessStatus.CREATED.getCode(), message, data, Instant.now())
        );
    }

    public static ResponseEntity<ApiResponse<Void>> error(ErrorStatus status, String message) {
        return ResponseEntity.status(status.getCode()).body(
                new ApiResponse<>(false, status.getCode(), message, null, Instant.now())
        );
    }
}
