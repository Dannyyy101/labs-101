package com.labs_101.backend.exception;

public abstract class BaseException extends RuntimeException {
    private final ErrorCode code;

    protected BaseException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}