package com.fkhr.gisapi.utils;

import org.springframework.http.HttpStatus;

public enum CustomError {
    FEATURE_NOT_FOUND(2002, "feature_not_found", HttpStatus.ACCEPTED),
    FEATURE_NOT_UPDATED(2003, "feature_not_updated", HttpStatus.ACCEPTED),
    FEATURE_ALREADY_EXIST(2004, "feature_already_exist", HttpStatus.ACCEPTED),
    ;

    private final int code;
    private final String message;
    private final HttpStatus status;

    CustomError(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
