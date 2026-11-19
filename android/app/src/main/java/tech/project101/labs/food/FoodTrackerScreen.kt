package tech.project101.labs.food

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import tech.project101.labs.core.compactFormatted
import tech.project101.labs.core.dayTitle
import tech.project101.labs.core.longDay
import tech.project101.labs.core.roundedFormatted
import tech.project101.labs.labsViewModel
import tech.project101.labs.ui.components.ConfirmDialog
import tech.project101.labs.ui.components.EmptyState
import tech.project101.labs.ui.components.ErrorDialog
import tech.project101.labs.ui.components.GoalBar
import tech.project101.labs.ui.components.LabsSection
import tech.project101.labs.ui.components.Loading
import tech.project101.labs.ui.theme.Labs
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Day overview of the tracked food, the Android counterpart of /foods/track. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodTrackerScreen(modifier: Modifier, onAdd: (Meal) -> Unit, onEdit: (TrackedFood) -> Unit) {
    val model = labsViewModel { FoodDayViewModel(it.foodService) }
    var deleting by remember { mutableStateOf<TrackedFood?>(null) }

    // also when coming back from adding or changing an entry
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.load() }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        containerColor = Labs.colors.canvas,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Labs.colors.canvas),
                title = {
                    Column {
                        Text(model.day.dayTitle(), fontWeight = FontWeight.Bold)
                        Text(model.day.longDay, style = MaterialTheme.typography.bodySmall, color = Labs.colors.inkMuted)
                    }
                },
                navigationIcon = {
                    Row {
                        IconButton(onClick = { model.showDay(-1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Vorheriger Tag")
                        }
                        IconButton(onClick = { model.showDay(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Nächster Tag")
                        }
                    }
                },
                actions = {
                    if (!model.isToday) TextButton(onClick = model::showToday) { Text("Heute") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAdd(Meal.current()) }, containerColor = Labs.colors.brand) {
                Icon(Icons.Filled.Add, "Essen hinzufügen", tint = Labs.colors.onBrand)
            }
        },
    ) { padding ->
        val loadError = model.loadError
        when {
            loadError != null && model.entries.isEmpty() -> EmptyState(
                Icons.Filled.WifiOff, "Keine Verbindung", loadError, "Erneut versuchen", model::load,
                Modifier.padding(padding),
            )
            !model.hasLoaded && model.isLoading -> Loading(Modifier.padding(padding))
            else -> PullToRefreshBox(
                isRefreshing = model.isLoading && model.hasLoaded,
                onRefresh = model::load,
                modifier = Modifier.padding(padding),
            ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        LabsSection {
                            DaySummary(consumed = { model.total(it) }, goals = model.goals)
                        }
                    }
                    Meal.entries.forEach { meal ->
                        item(key = meal.name) {
                            MealSection(
                                meal = meal,
                                entries = model.entries(meal),
                                remainingKcal = model.remainingKcal,
                                onAdd = { onAdd(meal) },
                                onSelect = onEdit,
                                onDelete = { deleting = it },
                            )
                        }
                    }
                }
            }
        }
    }

    deleting?.let { entry ->
        ConfirmDialog(
            title = "${entry.food.name} entfernen?",
            confirmLabel = "Entfernen",
            onConfirm = { model.delete(entry) },
            onDismiss = { deleting = null },
        )
    }
    ErrorDialog(model.actionError) { model.actionError = null }
}

val Meal.color: Color
    @Composable get() = when (this) {
        Meal.BREAKFAST -> Labs.colors.series2
        Meal.LUNCH -> Labs.colors.series1
        Meal.SNACK -> Labs.colors.series3
        Meal.DINNER -> Labs.colors.series4
    }

@Composable
private fun MealSection(
    meal: Meal,
    entries: List<TrackedFood>,
    remainingKcal: Double,
    onAdd: () -> Unit,
    onSelect: (TrackedFood) -> Unit,
    onDelete: (TrackedFood) -> Unit,
) {
    val kcal = entries.sumOf { it.amount(Nutrient.KCAL) }
    val firstEntry = entries.mapNotNull { it.createDate }.minOrNull()

    LabsSection(header = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(meal.color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(meal.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (firstEntry != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    firstEntry.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.bodySmall,
                    color = Labs.colors.inkMuted,
                )
            }
            Spacer(Modifier.weight(1f))
            Text("${kcal.roundedFormatted} kcal", style = MaterialTheme.typography.bodySmall, color = Labs.colors.inkMuted)
        }
    }) {
        if (entries.isEmpty()) {
            Text(
                "Noch nichts eingetragen. ${remainingKcal.coerceAtLeast(0.0).roundedFormatted} kcal stehen dir noch zur Verfügung.",
                style = MaterialTheme.typography.bodyMedium,
                color = Labs.colors.inkMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        entries.forEach { entry ->
            TrackedFoodRow(entry, onClick = { onSelect(entry) }, onDelete = { onDelete(entry) })
            HorizontalDivider(Modifier.padding(start = 16.dp), color = Labs.colors.line)
        }
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onAdd).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.AddCircle, contentDescription = null, tint = Labs.colors.brand)
            Spacer(Modifier.width(10.dp))
            Text("Hinzufügen", color = Labs.colors.brand)
        }
    }
}

@Composable
private fun TrackedFoodRow(entry: TrackedFood, onClick: () -> Unit, onDelete: () -> Unit) {
    val portion = entry.portion
    val amount = if (portion != null) {
        "${entry.amount.compactFormatted} ${portion.label} · ${entry.grams.roundedFormatted} g"
    } else {
        "${entry.amount.roundedFormatted} g"
    }
    val macros = "P ${entry.amount(Nutrient.PROTEIN).roundedFormatted} · " +
        "K ${entry.amount(Nutrient.CARBOHYDRATES).roundedFormatted} · F ${entry.amount(Nutrient.FAT).roundedFormatted}"

    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.food.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$amount · $macros", style = MaterialTheme.typography.bodySmall, color = Labs.colors.inkMuted)
        }
        Text(entry.amount(Nutrient.KCAL).roundedFormatted, color = Labs.colors.inkMuted)
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, "Entfernen", tint = Labs.colors.inkMuted)
        }
    }
}

/** Calories left and the macros of the day. */
@Composable
private fun DaySummary(consumed: (Nutrient) -> Double, goals: NutritionGoals) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CalorieRing(consumed = consumed(Nutrient.KCAL), goal = goals.kcal)
        HorizontalDivider(color = Labs.colors.line)
        MacroProgress("Protein", consumed(Nutrient.PROTEIN), goals.protein, Labs.colors.protein)
        MacroProgress("Kohlenhydrate", consumed(Nutrient.CARBOHYDRATES), goals.carbohydrates, Labs.colors.carbohydrates)
        MacroProgress("Fett", consumed(Nutrient.FAT), goals.fat, Labs.colors.fat)
    }
}

@Composable
private fun CalorieRing(consumed: Double, goal: Double) {
    val remaining = goal - consumed
    val progress = if (goal > 0) (consumed / goal).coerceIn(0.0, 1.0).toFloat() else 0f
    val track = Labs.colors.track
    val fill = if (remaining < 0) Labs.colors.danger else Labs.colors.brand

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(7.dp)) {
                val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                drawArc(track, 0f, 360f, useCenter = false, style = stroke, topLeft = Offset.Zero, size = Size(size.width, size.height))
                drawArc(fill, -90f, 360f * progress, useCenter = false, style = stroke)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(remaining.roundedFormatted, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("kcal übrig", style = MaterialTheme.typography.bodySmall, color = Labs.colors.inkMuted)
            }
        }
        Spacer(Modifier.width(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("Ziel", goal.roundedFormatted)
            Stat("Gegessen", consumed.roundedFormatted)
        }
    }
}

@Composable
private fun Stat(title: String, value: String) {
    Column {
        Text(title, style = MaterialTheme.typography.bodySmall, color = Labs.colors.inkMuted)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MacroProgress(title: String, consumed: Double, goal: Double, tint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${consumed.roundedFormatted} g", fontWeight = FontWeight.SemiBold)
            Text(" / ${goal.roundedFormatted} g", color = Labs.colors.inkMuted)
        }
        GoalBar(consumed, goal.coerceAtLeast(1.0), tint)
    }
    Spacer(Modifier.height(0.dp))
}
