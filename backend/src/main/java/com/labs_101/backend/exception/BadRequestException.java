package com.labs_101.backend.exception;

public class BadRequestException extends BaseException {

    protected BadRequestException(String code, Object... args) {
        super(code, args);
    }

    public static BadRequestException workout(Long id1, Long id2) {
        return new BadRequestException("error.pathVariable.idMismatch",
                id1, id2);
    }

    public static BadRequestException workoutSessionActive(Long activeId) {
        return new BadRequestException("error.workoutSession.active", activeId);
    }

    public static BadRequestException workoutSessionFinished(Long id) {
        return new BadRequestException("error.workoutSession.finished", id);
    }
}
