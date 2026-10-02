package com.tyson.digitalwallet.ddd.controller.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ErrorStatus {
    NOTFOUND(404),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    INTERNAL_ERROR(500);

    private int code;

    ErrorStatus(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }
}
