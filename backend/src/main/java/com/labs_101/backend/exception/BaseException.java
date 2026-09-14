package com.labs_101.backend.exception;

public abstract class BaseException extends RuntimeException {
    private final String code;
    private final Object[] args;

    protected BaseException(String code, Object... args) {
        super(code);
        this.code = code;
        this.args = args;
    }

    public String getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args;
    }
}