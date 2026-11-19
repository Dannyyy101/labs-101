package tech.project101.labs.food

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tech.project101.labs.core.userMessage
import kotlin.math.roundToLong

/** Searches our foods and the open food database by name, page by page, and looks up scanned barcodes. */
class FoodSearchViewModel(private val service: FoodService) : ViewModel() {
    var query by mutableStateOf("")
        private set
    var results by mutableStateOf<List<FoodSearchResult>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    private var nextPage: Int? = null
    private var searchJob: Job? = null

    val trimmedQuery: String get() = query.trim()

    fun updateQuery(text: String) {
        query = text
        val name = trimmedQuery
        searchJob?.cancel()
        if (name.isEmpty()) {
            results = emptyList()
            nextPage = null
            isLoading = false
            return
        }
        searchJob = viewModelScope.launch {
            // debounce typing, the job gets cancelled by the next keystroke
            delay(250)
            isLoading = true
            try {
                val page = service.search(name)
                results = page.content
                nextPage = if (page.last) null else page.number + 1
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.userMessage
            } finally {
                isLoading = false
            }
        }
    }

    fun loadNextPage() {
        val page = nextPage ?: return
        if (isLoading) return
        val name = trimmedQuery
        searchJob = viewModelScope.launch {
            isLoading = true
            try {
                val next = service.search(name, page)
                if (name != trimmedQuery) return@launch
                results = results + next.content
                nextPage = if (next.last) null else next.number + 1
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.userMessage
            } finally {
                isLoading = false
            }
        }
    }

    /** Looks the barcode up, the food is stored by the backend if it wasn't known yet. */
    fun lookUp(barcode: String, onFound: (Food) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                onFound(service.food(barcode))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.userMessage
            } finally {
                isLoading = false
            }
        }
    }
}

/** One food recognized in the text, before it is tracked. */
data class PreviewItem(
    val id: Int,
    val query: String,
    val amount: Double,
    val unit: String,
    val food: FoodDetails?,
    /** set while the food only exists in the open food database */
    val openFoodId: Long?,
    val portion: FoodPortion?,
    /** grams per unit, guessed while the user still has to confirm a new unit */
    val unitGrams: Double,
    val status: Status,
) {
    enum class Status {
        OK,

        /** the unit doesn't exist for the food yet, the user has to confirm its grams */
        NEW_UNIT,
        NOT_FOUND,
    }

    val isGramUnit: Boolean get() = unit in GRAM_UNITS
    val grams: Double get() = amount * unitGrams
    val kcal: Double get() = ((food?.per100g(Nutrient.KCAL) ?: 0.0) * grams / 100).roundToLong().toDouble()

    /** what gets sent to the backend: portions are tracked by count, everything else in grams */
    val trackedAmount: Double get() = if (portion == null) grams else amount

    companion object {
        // ml are counted 1:1 as grams
        val GRAM_UNITS = setOf("g", "ml")

        private val UNIT_GUESSES = mapOf(
            "tasse" to 200.0, "glas" to 200.0, "becher" to 250.0, "schüssel" to 300.0, "schale" to 250.0,
            "portion" to 150.0, "dose" to 400.0, "flasche" to 500.0, "scheibe" to 30.0, "handvoll" to 30.0,
            "esslöffel" to 15.0, "teelöffel" to 5.0, "stück" to 100.0, "riegel" to 40.0, "kugel" to 60.0, "prise" to 1.0,
        )

        fun guessedGrams(unit: String): Double = UNIT_GUESSES[unit.lowercase()] ?: 100.0

        fun sameLabel(a: String, b: String): Boolean = a.trim().lowercase() == b.trim().lowercase()

        fun from(extracted: ExtractedFood, id: Int): PreviewItem {
            val food = extracted.food
            val unit = extracted.unit ?: food?.portions?.firstOrNull { it.isDefault }?.label ?: "Stück"
            val portion = food?.portions?.firstOrNull { sameLabel(it.label, unit) }
            val (unitGrams, status) = when {
                food == null -> guessedGrams(unit) to Status.NOT_FOUND
                unit in GRAM_UNITS -> 1.0 to Status.OK
                portion != null -> portion.grams to Status.OK
                else -> guessedGrams(unit) to Status.NEW_UNIT
            }
            return PreviewItem(
                id, extracted.query, extracted.amount, unit, food, extracted.openFoodId,
                portion.takeIf { status == Status.OK && unit !in GRAM_UNITS }, unitGrams, status,
            )
        }
    }
}

/** Tracks several foods at once from a sentence like "2 Eier und eine Banane". */
class TextFoodViewModel(private val service: FoodService) : ViewModel() {
    var text by mutableStateOf("")
    var items by mutableStateOf<List<PreviewItem>>(emptyList())
        private set
    var isExtracting by mutableStateOf(false)
        private set
    var isSubmitting by mutableStateOf(false)
        private set
    var savingUnitIds by mutableStateOf<Set<Int>>(emptySet())
        private set
    var error by mutableStateOf<String?>(null)

    val pendingUnits: Int get() = items.count { it.status == PreviewItem.Status.NEW_UNIT }
    val notFound: Int get() = items.count { it.status == PreviewItem.Status.NOT_FOUND }
    val totalKcal: Double get() = items.sumOf { it.kcal }
    val canExtract: Boolean get() = !isExtracting && text.isNotBlank()
    val canSubmit: Boolean get() = items.isNotEmpty() && pendingUnits == 0 && notFound == 0 && !isSubmitting

    fun extract() {
        if (!canExtract) return
        error = null
        viewModelScope.launch {
            isExtracting = true
            try {
                items = service.extractFoods(text).mapIndexed { index, food -> PreviewItem.from(food, index) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = "Text konnte nicht erkannt werden"
            } finally {
                isExtracting = false
            }
        }
    }

    fun remove(item: PreviewItem) {
        items = items.filter { it.id != item.id }
    }

    fun update(id: Int, change: (PreviewItem) -> PreviewItem) {
        items = items.map { if (it.id == id) change(it) else it }
    }

    /** Stores the confirmed unit as new portion of the food. */
    fun saveUnit(item: PreviewItem) {
        val food = item.food ?: return
        error = null
        viewModelScope.launch {
            savingUnitIds = savingUnitIds + item.id
            try {
                // a portion needs a food, so open foods get copied into our foods first
                val imported = item.openFoodId?.let { service.importOpenFood(it) } ?: food
                val foodId = imported.id ?: throw IllegalStateException("Ungültige Antwort vom Server")

                val portions = service.addPortion(item.unit, item.unitGrams, foodId)
                val portion = portions.firstOrNull { PreviewItem.sameLabel(it.label, item.unit) }
                update(item.id) {
                    it.copy(
                        food = imported.copy(portions = portions),
                        openFoodId = null,
                        portion = portion,
                        status = if (portion == null) PreviewItem.Status.NEW_UNIT else PreviewItem.Status.OK,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = "Einheit „${item.unit}“ konnte nicht gespeichert werden"
            } finally {
                savingUnitIds = savingUnitIds - item.id
            }
        }
    }

    /** Tracks all items, calls [onDone] when all of them were tracked. */
    fun trackAll(meal: Meal, onDone: () -> Unit) {
        if (!canSubmit) return
        error = null
        viewModelScope.launch {
            isSubmitting = true
            try {
                for (item in items) {
                    val openFoodId = item.openFoodId
                    val foodId = item.food?.id
                    if (openFoodId != null) {
                        service.trackOpenFood(openFoodId, item.trackedAmount, item.portion?.id, meal)
                    } else if (foodId != null) {
                        service.track(foodId, item.trackedAmount, item.portion?.id, meal)
                    }
                    // tracked items leave the list, so a retry after an error doesn't track them twice
                    remove(item)
                }
                text = ""
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = "Eintragen fehlgeschlagen"
            } finally {
                isSubmitting = false
            }
        }
    }
}
