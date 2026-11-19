package tech.project101.labs.workout

import tech.project101.labs.core.compactFormatted
import tech.project101.labs.core.roundedFormatted

/** The numbers of the web dashboard, see frontend/app/workouts/stats.ts. */
object WorkoutStats {
    const val DEFAULT_REST = 90
    const val DEFAULT_CARDIO_MINUTES = 10

    /** 0 turns the rest timer off */
    val REST_OPTIONS = listOf(0, 30, 45, 60, 90, 120, 150, 180, 240, 300)

    /** weight times reps of the sets, in kg */
    fun volume(sets: List<SessionSet>): Double = sets.sumOf { it.reps * it.weightKg }

    /** "850 kg", tonnes from 10 t on so it stays short */
    fun formattedVolume(kg: Double): String =
        if (kg >= 10_000) "${(kg / 1000).compactFormatted} t" else "${kg.roundedFormatted} kg"

    /** "1:30", "0:45" or "Aus" */
    fun formattedRest(seconds: Int): String =
        if (seconds == 0) "Aus" else "%d:%02d".format(seconds / 60, seconds % 60)

    /** The done sets of the exercise in the newest of the sessions (sorted newest first) that contains it. */
    fun lastSets(exerciseId: Long?, sessions: List<WorkoutSession>): List<SessionSet>? {
        if (exerciseId == null) return null
        return sessions.firstNotNullOfOrNull { session ->
            session.exercises.firstOrNull { e -> e.exerciseId == exerciseId && e.sets.any { it.done } }
                ?.sets?.filter { it.done }
        }
    }

    /** The minutes of the cardio in the newest of the sessions (sorted newest first) that contains it. */
    fun lastCardioMinutes(exerciseId: Long?, sessions: List<WorkoutSession>): Int? {
        if (exerciseId == null) return null
        return sessions.firstNotNullOfOrNull { session ->
            session.exercises.firstOrNull { it.exerciseId == exerciseId && it.cardio?.done == true }?.cardio?.minutes
        }
    }

    /** The note of the last workout with the exercise, null when it had none, so a removed note doesn't come back. */
    fun lastNote(exerciseId: Long?, sessions: List<WorkoutSession>): String? {
        if (exerciseId == null) return null
        for (session in sessions) {
            val found = session.exercises.firstOrNull { it.exerciseId == exerciseId && it.isDone } ?: continue
            return found.note
        }
        return null
    }
}
