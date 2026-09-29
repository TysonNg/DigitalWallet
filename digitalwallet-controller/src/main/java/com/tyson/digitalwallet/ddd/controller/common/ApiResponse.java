package com.tyson.digitalwallet.ddd.controller.common;

import com.tyson.digitalwallet.ddd.controller.enums.ErrorStatus;
import com.tyson.digitalwallet.ddd.controller.enums.SuccessStatus;

public class ApiResponse<T> {
    protected int statusCode;
    protected String message;
    protected T data;

    public ApiResponse(int statusCode, String message, T data) {
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(
            String message,
            T data
    ) {
        return new ApiResponse<>(
                SuccessStatus.SUCCESS.getCode(),
                message,
                data
        );
    }

    public static <T> ApiResponse<T> created(
            String message,
            T data
    ) {
        return new ApiResponse<>(
                SuccessStatus.CREATED.getCode(),
                message,
                data
        );
    }

   public static ApiResponse<Void> error(
           int code,
           String message
   ) {
        return new ApiResponse<>(
                code,
                message,
                null
        );
   }
}
