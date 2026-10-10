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

    public static BadRequestException userSettingsExist(String userId) {
        return new BadRequestException("error.userSettings.exist", userId);
    }

    public static BadRequestException calorieGoal(Integer calorieGoal) {
        return new BadRequestException("error.userSettings.calorieGoal", calorieGoal);
    }

    public static BadRequestException notificationTitle() {
        return new BadRequestException("error.notification.title");
    }

    public static BadRequestException notificationLink(String link) {
        return new BadRequestException("error.notification.link", link);
    }

    public static BadRequestException meal(String meal) {
        return new BadRequestException("error.meal.invalid", meal);
    }

    public static BadRequestException foodImageDisabled() {
        return new BadRequestException("error.foodImage.disabled");
    }

    public static BadRequestException foodImageType(String contentType) {
        return new BadRequestException("error.foodImage.type", contentType);
    }

    public static BadRequestException foodImageSize(long maxMegabytes) {
        return new BadRequestException("error.foodImage.size", maxMegabytes);
    }

    public static BadRequestException foodImageDescription(int maxLength) {
        return new BadRequestException("error.foodImage.description", maxLength);
    }

    public static BadRequestException foodImageNotReady(Long id) {
        return new BadRequestException("error.foodImage.notReady", id);
    }

    public static BadRequestException foodImageItem() {
        return new BadRequestException("error.foodImage.item");
    }
}
