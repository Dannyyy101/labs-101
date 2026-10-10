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
}
