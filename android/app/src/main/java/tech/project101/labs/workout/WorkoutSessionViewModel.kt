package tech.project101.labs.workout

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.project101.labs.core.userMessage
import java.time.Instant
import java.time.temporal.ChronoUnit

/** A running workout, every change is saved shortly after it was made. */
class WorkoutSessionViewModel(
    private val id: Long,
    private val service: WorkoutService,
    private val prefs: SharedPreferences,
) : ViewModel() {
    enum class SaveState { SAVED, PENDING, SAVING, FAILED }

    data class Rest(val untilMillis: Long, val totalSeconds: Long)

    var session by mutableStateOf<WorkoutSession?>(null)
        private set
    var name by mutableStateOf("")
        private set
    var exercises by mutableStateOf<List<SessionExercise>>(emptyList())
        private set

    /** all exercises of the user, for the picker */
    var catalog by mutableStateOf<List<Exercise>>(emptyList())
        private set

    /** finished workouts of the last year, newest first, for the "last time" hints */
    var history by mutableStateOf<List<WorkoutSession>>(emptyList())
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var actionError by mutableStateOf<String?>(null)
    var saveState by mutableStateOf(SaveState.SAVED)
        private set
    var isFinishing by mutableStateOf(false)
        private set

    /** stored per workout, so it survives closing the workout or the app */
    var rest by mutableStateOf<Rest?>(null)
        private set

    /** counts up when a rest is over, for the haptic feedback */
    var restsFinished by mutableIntStateOf(0)
        private set

    private var saveJob: Job? = null
    private var restJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadError = null
        viewModelScope.launch {
            try {
                val now = Instant.now()
                val session = async { service.session(id) }
                val catalog = async { service.exercises() }
                val history = async { service.sessions(now.minus(365, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS)) }
                val loaded = session.await()
                this@WorkoutSessionViewModel.catalog = catalog.await()
                this@WorkoutSessionViewModel.history = history.await()
                name = loaded.name
                exercises = loaded.exercises
                this@WorkoutSessionViewModel.session = loaded
                restoreRest()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadError = e.userMessage
            }
        }
    }

    val allSets: List<SessionSet> get() = exercises.flatMap { it.sets }
    val doneSets: List<SessionSet> get() = allSets.filter { it.done }

    fun exercise(id: Long?): Exercise? = id?.let { catalog.firstOrNull { e -> e.id == it } }

    // editing

    fun rename(newName: String) {
        if (newName.isBlank()) return
        name = newName.trim()
        scheduleSave()
    }

    private fun change(transform: (List<SessionExercise>) -> List<SessionExercise>) {
        exercises = transform(exercises)
        scheduleSave()
    }

    fun updateExercise(key: String, transform: (SessionExercise) -> SessionExercise) =
        change { list -> list.map { if (it.key == key) transform(it) else it } }

    fun removeExercise(key: String) = change { list -> list.filter { it.key != key } }

    fun updateSet(exerciseKey: String, setKey: String, transform: (SessionSet) -> SessionSet) =
        updateExercise(exerciseKey) { e -> e.copy(sets = e.sets.map { if (it.key == setKey) transform(it) else it }) }

    fun removeSet(exerciseKey: String, setKey: String) =
        updateExercise(exerciseKey) { e -> e.copy(sets = e.sets.filter { it.key != setKey }) }

    /** Starts with the sets and the note of last time, or three empty sets. Cardio starts with the minutes of last time. */
    fun add(exercise: Exercise) {
        val note = WorkoutStats.lastNote(exercise.id, history)
        if (exercise.isCardio) {
            val minutes = WorkoutStats.lastCardioMinutes(exercise.id, history) ?: WorkoutStats.DEFAULT_CARDIO_MINUTES
            change { it + SessionExercise(exerciseId = exercise.id, name = exercise.name, note = note, sets = emptyList(), cardio = SessionCardio(minutes, false)) }
            return
        }
        val last = WorkoutStats.lastSets(exercise.id, history).orEmpty()
        val sets = if (last.isEmpty()) {
            List(3) { SessionSet(reps = 10, weightKg = 0.0) }
        } else {
            last.map { SessionSet(reps = it.reps, weightKg = it.weightKg, restSeconds = it.rest) }
        }
        change { it + SessionExercise(exerciseId = exercise.id, name = exercise.name, note = note, sets = sets) }
    }

    fun addSet(exerciseKey: String) = updateExercise(exerciseKey) { e ->
        // cardio has minutes, no sets
        if (e.cardio != null) return@updateExercise e
        val last = e.sets.lastOrNull()
        e.copy(sets = e.sets + SessionSet(reps = last?.reps ?: 10, weightKg = last?.weightKg ?: 0.0, restSeconds = last?.rest ?: WorkoutStats.DEFAULT_REST))
    }

    fun toggle(exerciseKey: String, setKey: String) {
        val set = exercises.firstOrNull { it.key == exerciseKey }?.sets?.firstOrNull { it.key == setKey } ?: return
        val done = !set.done
        updateSet(exerciseKey, setKey) { it.copy(done = done) }
        // checking a set off starts its rest
        if (done && set.rest > 0) startRest(set.rest.toLong())
    }

    // rest

    fun startRest(seconds: Long, total: Long = seconds) {
        rest = Rest(System.currentTimeMillis() + seconds * 1000, total)
        storeRest()
        watchRest()
    }

    fun adjustRest(bySeconds: Long) {
        val current = rest ?: return
        val left = ((current.untilMillis - System.currentTimeMillis()) / 1000 + bySeconds).coerceAtLeast(0)
        startRest(left, total = (current.totalSeconds + bySeconds).coerceAtLeast(15))
    }

    fun endRest() {
        restJob?.cancel()
        rest = null
        storeRest()
    }

    private fun watchRest() {
        val until = rest?.untilMillis ?: return
        restJob?.cancel()
        restJob = viewModelScope.launch {
            val left = until - System.currentTimeMillis()
            if (left > 0) {
                delay(left)
                restsFinished++
            }
            // the hint disappears a few seconds after the rest is over
            delay((5000 + minOf(0, left)).coerceAtLeast(0))
            rest = null
            storeRest()
        }
    }

    private val restKey get() = "workout-rest-$id"

    private fun storeRest() {
        val current = rest
        if (current == null) {
            prefs.edit().remove(restKey).apply()
            return
        }
        prefs.edit().putString(restKey, "${current.untilMillis};${current.totalSeconds}").apply()
    }

    /** a rest that is long over isn't worth showing anymore */
    private fun restoreRest() {
        val parts = prefs.getString(restKey, null)?.split(";")?.mapNotNull { it.toLongOrNull() }
        if (parts == null || parts.size != 2 || parts[0] - System.currentTimeMillis() < -5000) {
            prefs.edit().remove(restKey).apply()
            return
        }
        rest = Rest(parts[0], parts[1])
        watchRest()
    }

    // saving

    private fun scheduleSave() {
        saveState = SaveState.PENDING
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DELAY_MS)
            save()
        }
    }

    fun retrySave() {
        viewModelScope.launch { save() }
    }

    private suspend fun save() {
        val (savedName, savedExercises) = name to exercises
        saveState = SaveState.SAVING
        try {
            service.update(id, savedName, savedExercises)
            // only "saved" when nothing changed in the meantime
            if (savedName == name && savedExercises == exercises) saveState = SaveState.SAVED
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            saveState = SaveState.FAILED
        }
    }

    /** Saves a pending change right away, e.g. when the screen is closed or the app goes to the background. */
    fun saveNow() {
        if (saveState != SaveState.PENDING) return
        saveJob?.cancel()
        // not tied to the screen, closing it must not cancel the last save
        viewModelScope.launch { withContext(NonCancellable) { save() } }
    }

    fun finish(onDone: () -> Unit) {
        // the finish request carries the latest state, a save racing it would be rejected anyway
        saveJob?.cancel()
        isFinishing = true
        viewModelScope.launch {
            try {
                service.finish(id, name, exercises)
                endRest()
                saveState = SaveState.SAVED
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                actionError = e.userMessage
            } finally {
                isFinishing = false
            }
        }
    }

    fun discard(onDone: () -> Unit) {
        saveJob?.cancel()
        viewModelScope.launch {
            try {
                service.delete(id)
                endRest()
                saveState = SaveState.SAVED
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                actionError = e.userMessage
            }
        }
    }

    override fun onCleared() {
        // the view model scope is gone here, a pending change is sent by saveNow when the screen closes
        restJob?.cancel()
    }

    private companion object {
        const val SAVE_DELAY_MS = 700L
    }
}
