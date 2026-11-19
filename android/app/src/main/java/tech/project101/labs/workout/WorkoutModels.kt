package tech.project101.labs.workout

import tech.project101.labs.api.models.CardioItemDtoOrStrengthItemDto
import tech.project101.labs.api.models.ExerciseDto
import tech.project101.labs.api.models.SessionCardioDto
import tech.project101.labs.api.models.SessionExerciseDto
import tech.project101.labs.api.models.SessionSetDto
import tech.project101.labs.api.models.WorkoutDto
import tech.project101.labs.api.models.WorkoutSessionDto
import tech.project101.labs.core.compactFormatted
import java.time.Duration
import java.time.Instant
import java.util.UUID

// the DTOs of the backend (generated from api/openapi.json) as the models of the app, like on iOS

data class Exercise(
    val id: Long,
    val name: String,
    /** "Strength Training", "Cardio", "Running", … */
    val type: String?,
    val bodyPartSlugs: List<String>,
) {
    /** logged with minutes instead of sets, see CARDIO_TYPES in the web */
    val isCardio: Boolean get() = type in setOf("Cardio", "Running", "Swimming")

    /** the trained muscles in German, every muscle once */
    val muscles: List<String> get() = bodyPartSlugs.distinct().map(Muscle::name)

    constructor(dto: ExerciseDto) : this(dto.id, dto.name, dto.type, dto.bodyParts.map { it.slug })
}

/** A workout template, see WorkoutController in the backend. */
data class WorkoutTemplate(
    val id: Long,
    val name: String,
    val items: List<Item>,
    /** name of the creator, null if unknown */
    val ownerName: String?,
    /** a friend shared the template with the user, it can be trained but not changed */
    val sharedWithMe: Boolean,
) {
    data class Item(val exercise: Exercise, val setCount: Int, val minutes: Int?)

    val setCount: Int get() = items.sumOf { it.setCount }
    val cardioMinutes: Int get() = items.sumOf { it.minutes ?: 0 }

    constructor(dto: WorkoutDto) : this(
        dto.id, dto.name, dto.workoutExercises.map(::item), dto.ownerName, dto.sharedWithMe,
    )

    private companion object {
        fun item(dto: CardioItemDtoOrStrengthItemDto) =
            Item(Exercise(dto.exercise), dto.sets.size, dto.minutes.takeIf { dto.type == "CARDIO_EXERCISE" })
    }
}

/** A workout of the user, [endedAt] is null while it is still running. */
data class WorkoutSession(
    val id: Long,
    /** the template it was started from, null for a free workout */
    val workoutId: Long?,
    val name: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val exercises: List<SessionExercise>,
) {
    val allSets: List<SessionSet> get() = exercises.flatMap { it.sets }
    val doneSets: List<SessionSet> get() = allSets.filter { it.done }

    /** minutes of done cardio */
    val cardioMinutes: Int get() = exercises.sumOf { e -> e.cardio?.takeIf { it.done }?.minutes ?: 0 }

    /** seconds from the start to the end, or to now while it is running */
    fun durationSeconds(now: Instant = Instant.now()): Long =
        Duration.between(startedAt, endedAt ?: now).seconds.coerceAtLeast(0)

    constructor(dto: WorkoutSessionDto) : this(
        dto.id, dto.workoutId, dto.name, dto.startedAt.toInstant(), dto.endedAt?.toInstant(),
        dto.exercises.map(::SessionExercise),
    )
}

data class SessionExercise(
    /** only for the ui, not sent to the backend */
    val key: String = UUID.randomUUID().toString(),
    val exerciseId: Long?,
    /** kept so the history still reads right after the exercise is renamed or deleted */
    val name: String,
    /** a short note of the user, e.g. the seat height, null when there is none */
    val note: String? = null,
    val sets: List<SessionSet>,
    /** minutes of cardio like running or rowing, null for a strength exercise with sets */
    val cardio: SessionCardio? = null,
) {
    /** kept when the workout is finished */
    val isDone: Boolean
        get() = cardio?.let { it.done && it.minutes > 0 } ?: sets.any { it.done }

    constructor(dto: SessionExerciseDto) : this(
        exerciseId = dto.exerciseId,
        name = dto.name,
        note = dto.note,
        sets = dto.sets.map(::SessionSet),
        cardio = dto.cardio?.let { SessionCardio(it.minutes, it.done) },
    )

    fun toDto() = SessionExerciseDto(
        cardio = cardio?.let { SessionCardioDto(done = it.done, minutes = it.minutes) },
        exerciseId = exerciseId,
        name = name,
        note = note,
        sets = sets.map { it.toDto() },
    )
}

/** Only done cardio counts, like only done sets. */
data class SessionCardio(val minutes: Int, val done: Boolean)

data class SessionSet(
    /** only for the ui, not sent to the backend */
    val key: String = UUID.randomUUID().toString(),
    val reps: Int,
    val weightKg: Double,
    val rpe: Int? = null,
    val done: Boolean = false,
    /** the rest after the set, null for the default */
    val restSeconds: Int? = WorkoutStats.DEFAULT_REST,
) {
    val rest: Int get() = restSeconds ?: WorkoutStats.DEFAULT_REST

    /** "10 × 60 kg", "12 Wdh" without weight */
    val formatted: String get() = if (weightKg > 0) "$reps × ${weightKg.compactFormatted} kg" else "$reps Wdh"

    constructor(dto: SessionSetDto) : this(
        reps = dto.reps, weightKg = dto.weightKg, rpe = dto.rpe, done = dto.done, restSeconds = dto.restSeconds,
    )

    fun toDto() = SessionSetDto(done = done, reps = reps, restSeconds = restSeconds, rpe = rpe, weightKg = weightKg)
}

object Muscle {
    private val names = mapOf(
        "chest" to "Brust", "biceps" to "Bizeps", "triceps" to "Trizeps", "deltoids" to "Schultern", "abs" to "Bauch",
        "obliques" to "Seitlicher Bauch", "quadriceps" to "Quadrizeps", "hamstring" to "Beinbeuger", "gluteal" to "Gesäß",
        "calves" to "Waden", "upper-back" to "Oberer Rücken", "lower-back" to "Unterer Rücken", "trapezius" to "Trapez",
        "forearm" to "Unterarme", "adductors" to "Adduktoren", "tibialis" to "Schienbein", "neck" to "Nacken",
        "knees" to "Knie", "ankles" to "Knöchel", "hands" to "Hände", "feet" to "Füße", "head" to "Kopf", "hair" to "Kopf",
    )

    fun name(slug: String): String = names[slug] ?: slug
}
