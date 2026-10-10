package com.labs_101.backend.exception;

public class TooManyRequestsException extends BaseException {

    protected TooManyRequestsException(String code, Object... args) {
        super(code, args);
    }

    public static TooManyRequestsException foodImageLimit(int limit) {
        return new TooManyRequestsException("error.foodImage.limit", limit);
    }
}
