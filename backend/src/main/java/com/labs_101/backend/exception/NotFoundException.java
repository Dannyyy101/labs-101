package com.labs_101.backend.exception;

public class NotFoundException extends BaseException {

    protected NotFoundException(ErrorCode code, String message) {
        super(code, message);
    }

    public static NotFoundException workout(Long id) {
        return new NotFoundException(ErrorCode.WORKOUT_NOT_FOUND,
                "Workout %s not found".formatted(id));
    }

}
