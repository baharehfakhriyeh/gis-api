package com.fkhr.gisapi.utils;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public class CustomException extends RuntimeException{
    private HttpStatus status;
    private String message;
    private int code;
    private Throwable cause;
    private LocalDateTime time;

    public CustomException() {
    }

    public CustomException(HttpStatus status, String message, int code, Throwable cause) {
        this.status = status;
        this.message = message;
        this.code = code;
        this.cause = cause;
        this.time = LocalDateTime.now();
    }

    public CustomException(CustomError customError, Throwable cause){
        this.status = customError.getStatus();
        message = customError.getMessage();
        code = customError.getCode();
        this.cause = cause;
        this.time = LocalDateTime.now();
    }

    public CustomException(HttpStatus status, Throwable cause){
        this.status = status;
        message = cause.getMessage();
        code = status.value();
        this.cause = cause;
        this.time = LocalDateTime.now();
    }

    public CustomException(CustomError customError){
        this.status = customError.getStatus();
        message = customError.getMessage();
        code = customError.getCode();
        this.cause = null;
        this.time = LocalDateTime.now();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}
