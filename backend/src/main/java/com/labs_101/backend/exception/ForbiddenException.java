package com.labs_101.backend.exception;

public class ForbiddenException extends BaseException {

    protected ForbiddenException(String code, Object... args) {
        super(code, args);
    }

    public static ForbiddenException workout(Long id) {
        return new ForbiddenException("error.workout.forbidden", id);
    }
}
