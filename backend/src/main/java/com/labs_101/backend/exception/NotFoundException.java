package com.labs_101.backend.exception;

public class NotFoundException extends BaseException {

    protected NotFoundException(String code, Object... args) {
        super(code, args);
    }

    public static NotFoundException workout(Long id) {
        return new NotFoundException("",
                "Workout %s not found".formatted(id));
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

}
