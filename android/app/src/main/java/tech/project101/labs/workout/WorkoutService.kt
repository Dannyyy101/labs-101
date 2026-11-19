package tech.project101.labs.workout

import tech.project101.labs.api.apis.WorkoutControllerApi
import tech.project101.labs.api.apis.WorkoutSessionControllerApi
import tech.project101.labs.api.models.CreateWorkoutSessionDto
import tech.project101.labs.api.models.UpdateWorkoutSessionDto
import tech.project101.labs.core.ApiClient
import tech.project101.labs.core.ApiException
import tech.project101.labs.core.ensureSuccess
import tech.project101.labs.core.result
import tech.project101.labs.core.resultOrNull
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** The workout endpoints of the backend, for the signed in user. */
class WorkoutService(api: ApiClient) {
    private val workouts = api.create<WorkoutControllerApi>()
    private val sessions = api.create<WorkoutSessionControllerApi>()

    // templates and exercises

    suspend fun templates(): List<WorkoutTemplate> = workouts.workoutGetAllWorkouts().result().map(::WorkoutTemplate)

    suspend fun exercises(): List<Exercise> = workouts.workoutGetAllExercises().result().map(::Exercise)

    // sessions

    /** Finished workouts started in the range, newest first. */
    suspend fun sessions(from: Instant, to: Instant): List<WorkoutSession> =
        sessions.workoutSessionGetSessions(from.utc(), to.utc()).result().map(::WorkoutSession)

    suspend fun session(id: Long): WorkoutSession = WorkoutSession(sessions.workoutSessionGetSession(id).result())

    /** The running workout, null when there is none. */
    suspend fun activeSession(): WorkoutSession? = sessions.workoutSessionGetActive().resultOrNull()?.let(::WorkoutSession)

    /**
     * Starts a workout from the template, or without exercises when [workoutId] is null.
     * Fails with status 400 while another workout is running.
     */
    suspend fun start(workoutId: Long?): WorkoutSession =
        WorkoutSession(sessions.workoutSessionStart(CreateWorkoutSessionDto(workoutId = workoutId)).result())

    /** Saves the current state of a running workout. */
    suspend fun update(id: Long, name: String, exercises: List<SessionExercise>) {
        sessions.workoutSessionUpdate(id, UpdateWorkoutSessionDto(exercises.map { it.toDto() }, name)).ensureSuccess()
    }

    /** Saves the last state and ends the workout, sets that weren't checked off are dropped. */
    suspend fun finish(id: Long, name: String, exercises: List<SessionExercise>) {
        sessions.workoutSessionFinish(id, UpdateWorkoutSessionDto(exercises.map { it.toDto() }, name)).ensureSuccess()
    }

    suspend fun delete(id: Long) {
        sessions.workoutSessionDelete(id).ensureSuccess()
    }

    private fun Instant.utc(): OffsetDateTime = atOffset(ZoneOffset.UTC)
}

val Throwable.isAnotherWorkoutRunning: Boolean get() = this is ApiException && status == 400
