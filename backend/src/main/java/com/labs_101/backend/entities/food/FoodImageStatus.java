package com.labs_101.backend.entities.food;

/**
 * PENDING until the job analyzed the photo, READY while the user still has to
 * check it, ACCEPTED/REJECTED once they did. FAILED after the last attempt of
 * the job, failed photos don't count against the daily limit.
 */
public enum FoodImageStatus {
    PENDING,
    READY,
    FAILED,
    ACCEPTED,
    REJECTED
}
