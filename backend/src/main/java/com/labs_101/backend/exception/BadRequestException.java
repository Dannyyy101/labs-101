package com.labs_101.backend.exception;

public class BadRequestException extends BaseException {

    protected BadRequestException(String code, Object... args) {
        super(code, args);
    }

    public static BadRequestException workout(Long id1, Long id2) {
        return new BadRequestException("error.pathVariable.idMismatch",
                id1, id2);
    }
}
