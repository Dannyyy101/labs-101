package com.labs_101.backend.exception;

public class BadRequestException extends BaseException {

    protected BadRequestException(ErrorCode code, String message) {
        super(code, message);
    }

    public static BadRequestException workout(Long id1, Long id2) {
        return new BadRequestException(ErrorCode.PATH_VARIABLE_ID_AND_REQUEST_BODY_ID_DO_NOT_MATCH,
                "Path variable with %s does not match id in request body: %s".formatted(id1, id2));
    }
}
