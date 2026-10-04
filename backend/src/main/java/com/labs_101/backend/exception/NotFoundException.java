package com.labs_101.backend.exception;

import java.util.UUID;

public class NotFoundException extends BaseException {

    protected NotFoundException(String code, Object... args) {
        super(code, args);
    }

    public static NotFoundException workout(Long id) {
        return new NotFoundException("error.workout.notFound", id);
    }

    public static NotFoundException food(Long id) {
        return new NotFoundException("error.food.notFound", id);
    }

    public static NotFoundException foodPortion(Long id) {
        return new NotFoundException("error.foodPortion.notFound", id);
    }

    public static NotFoundException foodByBarcode(String code) {
        return new NotFoundException("error.foodByBarcode.notFound", code);
    }

    public static NotFoundException openFood(Long id) {
        return new NotFoundException("error.openFood.notFound", id);
    }

    public static NotFoundException trackedFood(Long id) {
        return new NotFoundException("error.trackedFood.notFound", id);
    }

    public static NotFoundException healthWriteRequest(Long id) {
        return new NotFoundException("error.healthWriteRequest.notFound", id);
    }

    public static NotFoundException exercise(Long id) {
        return new NotFoundException("error.exercise.notFound", id);
    }

    public static NotFoundException workoutSession(Long id) {
        return new NotFoundException("error.workoutSession.notFound", id);
    }

    public static NotFoundException run(UUID id) {
        return new NotFoundException("error.run.notFound", id);
    }

}
