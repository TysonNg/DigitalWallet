package com.tyson.digitalwallet.ddd.controller.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SuccessStatus {
    SUCCESS(200),
    CREATED(201);

    private final int code;

    SuccessStatus(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

}
