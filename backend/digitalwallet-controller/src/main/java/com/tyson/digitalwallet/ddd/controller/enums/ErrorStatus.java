package com.tyson.digitalwallet.ddd.controller.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ErrorStatus {
    NOTFOUND(404),
    BAD_REQUEST(400),
    CONFLICT(409),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    TOO_MANY_REQUESTS(429),
    INTERNAL_ERROR(500),
    SERVICE_UNAVAILABLE(503);

    private int code;

    ErrorStatus(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }
}
