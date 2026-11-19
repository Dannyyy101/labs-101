package tech.project101.labs.food

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tech.project101.labs.core.userMessage
import java.time.LocalDate

/** The food tracked on one day. */
class FoodDayViewModel(private val service: FoodService) : ViewModel() {
    var day by mutableStateOf(LocalDate.now())
        private set
    var entries by mutableStateOf<List<TrackedFood>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var hasLoaded by mutableStateOf(false)
        private set

    /** set when loading the day failed, changes are reported through [actionError] */
    var loadError by mutableStateOf<String?>(null)
        private set
    var actionError by mutableStateOf<String?>(null)

    val goals = NutritionGoals.DEFAULT

    private var loadJob: Job? = null

    val isToday: Boolean get() = day == LocalDate.now()

    fun showDay(byAdding: Long) {
        day = day.plusDays(byAdding)
        load()
    }

    fun showToday() {
        day = LocalDate.now()
        load()
    }

    fun load() {
        val requestedDay = day
        // a newer load replaces the running one
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading = true
            try {
                val loaded = service.trackedFood(requestedDay)
                if (requestedDay != day) return@launch
                entries = loaded
                loadError = null
                hasLoaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (requestedDay != day) return@launch
                loadError = e.userMessage
            } finally {
                isLoading = false
            }
        }
    }

    fun delete(entry: TrackedFood) {
        // optimistic, so the row disappears right away
        val previous = entries
        entries = entries.filter { it.id != entry.id }
        viewModelScope.launch {
            try {
                service.delete(entry)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                entries = previous
                actionError = e.userMessage
            }
        }
    }

    // totals

    fun entries(meal: Meal): List<TrackedFood> = entries.filter { it.meal == meal }

    fun total(nutrient: Nutrient, of: List<TrackedFood> = entries): Double =
        of.filter { it.amount > 0 }.sumOf { it.amount(nutrient) }

    // without health connect there is no burned energy yet, so the goal is the whole budget
    val remainingKcal: Double get() = goals.kcal - total(Nutrient.KCAL)
}
