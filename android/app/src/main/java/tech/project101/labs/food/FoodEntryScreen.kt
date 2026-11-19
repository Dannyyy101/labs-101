package tech.project101.labs.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import tech.project101.labs.core.compactFormatted
import tech.project101.labs.core.inputFormatted
import tech.project101.labs.core.parseDecimal
import tech.project101.labs.core.roundedFormatted
import tech.project101.labs.core.userMessage
import tech.project101.labs.labsViewModel
import tech.project101.labs.ui.FoodEntryRoute
import tech.project101.labs.ui.components.ConfirmDialog
import tech.project101.labs.ui.components.EmptyState
import tech.project101.labs.ui.components.ErrorDialog
import tech.project101.labs.ui.components.LabsSection
import tech.project101.labs.ui.components.Loading
import tech.project101.labs.ui.theme.Labs

/** Tracks a food or changes / deletes a tracked one. */
class FoodEntryViewModel(private val route: FoodEntryRoute, private val service: FoodService) : ViewModel() {
    var food by mutableStateOf<FoodDetails?>(null)
        private set
    var entry by mutableStateOf<TrackedFood?>(null)
        private set
    var amountText by mutableStateOf("100")
    var portionId by mutableStateOf<Long?>(null)
    var meal by mutableStateOf(Meal.valueOf(route.meal))
    var loadError by mutableStateOf<String?>(null)
        private set
    var actionError by mutableStateOf<String?>(null)
    var isSaving by mutableStateOf(false)
        private set

    val isEditing: Boolean get() = route.trackedFoodId != null
    val amount: Double get() = amountText.parseDecimal() ?: 0.0
    val portion: FoodPortion? get() = food?.portions?.firstOrNull { it.id == portionId }
    val grams: Double get() = amount * (portion?.grams ?: 1.0)

    init {
        load()
    }

    fun load() {
        loadError = null
        viewModelScope.launch {
            try {
                val trackedFoodId = route.trackedFoodId
                val openFoodId = route.openFoodId
                val foodId = route.foodId
                when {
                    trackedFoodId != null -> {
                        val tracked = service.trackedFood(trackedFoodId)
                        val details = service.details(tracked.food.id)
                        entry = tracked
                        amountText = tracked.amount.inputFormatted
                        portionId = tracked.portion?.id?.takeIf { id -> details.portions.any { it.id == id } }
                        meal = tracked.meal
                        food = details
                    }
                    openFoodId != null -> food = service.openFood(openFoodId).also { amountText = "100" }
                    foodId != null -> food = service.details(foodId).also { amountText = (it.lastAmount ?: 100.0).inputFormatted }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadError = e.userMessage
            }
        }
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            isSaving = true
            try {
                val tracked = entry
                val openFoodId = route.openFoodId
                val foodId = route.foodId
                when {
                    tracked != null -> service.update(tracked, amount, portionId, meal)
                    openFoodId != null -> service.trackOpenFood(openFoodId, amount, portionId, meal)
                    foodId != null -> service.track(foodId, amount, portionId, meal)
                }
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                actionError = e.userMessage
            } finally {
                isSaving = false
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        val tracked = entry ?: return
        viewModelScope.launch {
            isSaving = true
            try {
                service.delete(tracked)
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                actionError = e.userMessage
            } finally {
                isSaving = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodEntryScreen(route: FoodEntryRoute, onBack: () -> Unit, onDone: () -> Unit) {
    val model = labsViewModel(key = route.toString()) { FoodEntryViewModel(route, it.foodService) }
    var confirmDelete by remember { mutableStateOf(false) }
    val food = model.food

    Scaffold(
        containerColor = Labs.colors.canvas,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Labs.colors.canvas),
                title = { Text(food?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
                actions = {
                    TextButton(
                        onClick = { model.save(onDone) },
                        enabled = food != null && model.amount > 0 && !model.isSaving,
                    ) {
                        Icon(Icons.Filled.Check, null)
                        Text(if (model.isEditing) "Speichern" else "Hinzufügen")
                    }
                },
            )
        },
    ) { padding ->
        val loadError = model.loadError
        when {
            food != null -> EntryForm(model, food, Modifier.padding(padding), onDelete = { confirmDelete = true })
            loadError != null -> EmptyState(
                Icons.Filled.Warning, "Nicht geladen", loadError, "Erneut versuchen", model::load, Modifier.padding(padding),
            )
            else -> Loading(Modifier.padding(padding))
        }
    }

    if (confirmDelete && food != null) {
        ConfirmDialog(
            title = "${food.name} entfernen?",
            confirmLabel = "Entfernen",
            onConfirm = { model.delete(onDone) },
            onDismiss = { confirmDelete = false },
        )
    }
    ErrorDialog(model.actionError) { model.actionError = null }
}

@Composable
private fun EntryForm(model: FoodEntryViewModel, food: FoodDetails, modifier: Modifier, onDelete: () -> Unit) {
    val grams = model.grams

    Column(
        modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LabsSection {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(food.amount(Nutrient.KCAL, grams).roundedFormatted, fontSize = 44.sp, fontWeight = FontWeight.SemiBold)
                Text("kcal", color = Labs.colors.inkMuted)
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Macro("Protein", food.amount(Nutrient.PROTEIN, grams), Labs.colors.protein)
                    Macro("Kohlenhydrate", food.amount(Nutrient.CARBOHYDRATES, grams), Labs.colors.carbohydrates)
                    Macro("Fett", food.amount(Nutrient.FAT, grams), Labs.colors.fat)
                }
            }
        }

        LabsSection(title = "Menge") {
            if (food.portions.isNotEmpty()) {
                UnitOption("Gramm", selected = model.portionId == null) { model.portionId = null }
                food.portions.forEach { portion ->
                    UnitOption("${portion.label} (${portion.grams.compactFormatted} g)", selected = model.portionId == portion.id) {
                        model.portionId = portion.id
                    }
                }
            }
            OutlinedTextField(
                value = model.amountText,
                onValueChange = { text -> model.amountText = text.filter { it.isDigit() || it == ',' || it == '.' } },
                label = { Text("Menge") },
                suffix = { Text(model.portion?.label ?: "g") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }

        LabsSection {
            MealPicker(model.meal, { model.meal = it }, Modifier.padding(16.dp))
        }

        if (model.isEditing) {
            LabsSection {
                TextButton(onClick = onDelete, enabled = !model.isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text("Eintrag löschen", color = Labs.colors.danger)
                }
            }
        }
    }
}

@Composable
private fun UnitOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

@Composable
private fun Macro(title: String, value: Double, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = Labs.colors.inkMuted, fontWeight = FontWeight.SemiBold)
        Text("${value.compactFormatted} g", style = MaterialTheme.typography.titleMedium, color = tint, fontWeight = FontWeight.SemiBold)
    }
}
