package tech.project101.labs.overview

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tech.project101.labs.api.apis.HealthOverviewControllerApi
import tech.project101.labs.api.models.DayScoreDto
import tech.project101.labs.api.models.HealthOverviewDto
import tech.project101.labs.core.ApiClient
import tech.project101.labs.core.dayTitle
import tech.project101.labs.core.decimalFormatted
import tech.project101.labs.core.longDay
import tech.project101.labs.core.result
import tech.project101.labs.core.roundedFormatted
import tech.project101.labs.core.sleepDuration
import tech.project101.labs.core.userMessage
import tech.project101.labs.labsViewModel
import tech.project101.labs.ui.components.EmptyState
import tech.project101.labs.ui.components.GoalBar
import tech.project101.labs.ui.components.LabeledRow
import tech.project101.labs.ui.components.LabsSection
import tech.project101.labs.ui.components.Loading
import tech.project101.labs.ui.theme.Labs
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

// sleep, recovery and strain of a day, see HealthOverviewController in the backend.
// times in seconds, energy in kcal, distances in meters, scores 0–100.

/** The health overview endpoint of the backend, for the signed in user. */
class OverviewService(api: ApiClient) {
    private val overview = api.create<HealthOverviewControllerApi>()

    /** The overview of the day in the time zone of the phone. */
    suspend fun overview(day: LocalDate): HealthOverviewDto =
        overview.healthOverviewGetOverview(day, ZoneId.systemDefault().id).result()
}

class OverviewViewModel(private val service: OverviewService) : ViewModel() {
    var day by mutableStateOf(LocalDate.now())
        private set
    var overview by mutableStateOf<HealthOverviewDto?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set

    private var loadJob: Job? = null

    val isToday: Boolean get() = day == LocalDate.now()

    fun showDay(byAdding: Long) = show(day.plusDays(byAdding))

    fun showToday() = show(LocalDate.now())

    fun show(date: LocalDate) {
        // there is nothing to show in the future
        day = minOf(date, LocalDate.now())
        load()
    }

    fun load() {
        val requestedDay = day
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading = true
            try {
                val loaded = service.overview(requestedDay)
                if (requestedDay != day) return@launch
                overview = loaded
                loadError = null
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
}

private object Goals {
    const val STEPS = 10_000.0
    const val EXERCISE = 30.0
    const val STAND = 12.0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(modifier: Modifier) {
    val model = labsViewModel { OverviewViewModel(it.overviewService) }
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
                        IconButton(onClick = { model.showDay(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Vorheriger Tag") }
                        IconButton(onClick = { model.showDay(1) }, enabled = !model.isToday) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Nächster Tag")
                        }
                    }
                },
                actions = { if (!model.isToday) TextButton(onClick = model::showToday) { Text("Heute") } },
            )
        },
    ) { padding ->
        val overview = model.overview
        val loadError = model.loadError
        when {
            loadError != null && overview == null -> EmptyState(
                Icons.Filled.WifiOff, "Keine Verbindung", loadError, "Erneut versuchen", model::load, Modifier.padding(padding),
            )
            overview == null -> Loading(Modifier.padding(padding))
            else -> PullToRefreshBox(model.isLoading, model::load, Modifier.padding(padding)) {
                OverviewList(overview, onSelectDay = model::show)
            }
        }
    }
}

@Composable
private fun OverviewList(overview: HealthOverviewDto, onSelectDay: (LocalDate) -> Unit) {
    val recovery = overview.recovery
    val strain = overview.strain
    val activity = overview.activity
    val sleep = overview.sleep
    val hasData = overview.history.any { it.sleep != null || it.steps > 0 || it.strain != null }
    val time = DateTimeFormatter.ofPattern("HH:mm")
    val zone = ZoneId.systemDefault()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!hasData) {
            item {
                LabsSection {
                    EmptyState(
                        Icons.Filled.MonitorHeart,
                        "Noch keine Gesundheitsdaten",
                        "Schlaf, Herzfrequenz und Aktivität kommen bisher aus Apple Health. Auf Android werden sie noch nicht synchronisiert.",
                    )
                }
            }
        }

        item {
            LabsSection {
                Row(Modifier.fillMaxWidth().height(150.dp).padding(vertical = 12.dp)) {
                    ScoreColumn("Belastung", strain.score, Labs.colors.brand, Modifier.weight(1f))
                    VerticalDivider(color = Labs.colors.line)
                    ScoreColumn("Erholung", recovery.score, recoveryColor(recovery.score), Modifier.weight(1f))
                    VerticalDivider(color = Labs.colors.line)
                    ScoreColumn("Schlaf", sleep?.score, Labs.colors.brand, Modifier.weight(1f))
                }
                Text(
                    recoveryText(recovery.score),
                    style = MaterialTheme.typography.bodySmall,
                    color = Labs.colors.inkMuted,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            }
        }

        item {
            LabsSection(
                title = "Schlaf",
                trailing = sleep?.let { "${it.start.atZoneSameInstant(zone).format(time)} – ${it.end.atZoneSameInstant(zone).format(time)}" },
            ) {
                if (sleep == null) {
                    Text("Für diese Nacht wurde kein Schlaf aufgezeichnet.", color = Labs.colors.inkMuted, modifier = Modifier.padding(16.dp))
                } else {
                    LabeledRow("Geschlafen", sleep.asleep.sleepDuration)
                    LabeledRow("Bedarf", sleep.need.sleepDuration)
                    LabeledRow("Tief", sleep.deep.sleepDuration)
                    LabeledRow("Kern", sleep.core.sleepDuration)
                    LabeledRow("REM", sleep.rem.sleepDuration)
                    LabeledRow("Wach", sleep.awake.sleepDuration)
                }
            }
        }

        item {
            LabsSection(title = "Vitalwerte", trailing = "vs. Ø 30 Tage") {
                VitalRow("Herzfrequenzvariabilität", recovery.hrv, recovery.hrvBaseline, "ms")
                VitalRow("Ruhepuls", recovery.restingHeartRate, recovery.restingHeartRateBaseline, "bpm")
                VitalRow("Atemfrequenz", recovery.respiratoryRate, recovery.respiratoryRateBaseline, "/min", decimals = 1)
                VitalRow("Blutsauerstoff", recovery.oxygenSaturation?.let { it * 100 }, null, "%", decimals = 1)
            }
        }

        item {
            LabsSection(title = "Aktivität") {
                GoalRow("Schritte", activity.steps, Goals.STEPS, activity.steps.roundedFormatted)
                GoalRow("Trainingsminuten", activity.exerciseMinutes, Goals.EXERCISE, "${activity.exerciseMinutes.roundToInt()} min")
                GoalRow("Stehstunden", activity.standHours.toDouble(), Goals.STAND, "${activity.standHours} h")
                HorizontalDivider(color = Labs.colors.line)
                LabeledRow("Aktive Energie", "${activity.activeEnergy.roundedFormatted} kcal")
                LabeledRow("Gesamt", "${(activity.activeEnergy + activity.basalEnergy).roundedFormatted} kcal")
                LabeledRow("Distanz", "${(activity.distance / 1000).decimalFormatted(1)} km")
                LabeledRow("Gegessen", activity.energyConsumed?.let { "${it.roundedFormatted} kcal" } ?: "–")
            }
        }

        if (overview.history.isNotEmpty()) {
            item {
                LabsSection(title = "Verlauf") {
                    TrendBars("Erholung", overview, { it.recovery?.toDouble() }, { "${it.roundToInt()} %" }, 100.0, onSelectDay)
                    TrendBars("Belastung", overview, { it.strain?.toDouble() }, { "${it.roundToInt()} %" }, 100.0, onSelectDay)
                    TrendBars("Schlaf", overview, { it.sleep }, { it.sleepDuration }, null, onSelectDay)
                }
            }
        }

        item {
            LabsSection(title = "Körper") {
                LabeledRow("VO₂max", overview.vitals.vo2Max?.let { "${it.decimalFormatted(1)} ml/kg·min" } ?: "–")
                val change = overview.vitals.bodyMassChange
                val changeText = if (change != null && abs(change) >= 0.05) {
                    " (${if (change > 0) "+" else "−"}${abs(change).decimalFormatted(1)} kg in 30 Tagen)"
                } else {
                    ""
                }
                LabeledRow("Gewicht", (overview.vitals.bodyMass?.let { "${it.decimalFormatted(1)} kg" } ?: "–") + changeText)
            }
        }
    }
}

@Composable
private fun recoveryColor(score: Int?): Color = when {
    score == null -> Labs.colors.track
    score >= 67 -> Labs.colors.successBar
    score >= 34 -> Labs.colors.warning
    else -> Labs.colors.danger
}

private fun recoveryText(score: Int?): String = when {
    score == null -> "Noch zu wenige Nächte zum Vergleichen"
    score >= 67 -> "Bereit für hohe Belastung"
    score >= 34 -> "Moderat belastbar"
    else -> "Heute auf Erholung achten"
}

@Composable
private fun ScoreColumn(title: String, score: Int?, color: Color, modifier: Modifier) {
    val track = Labs.colors.track
    Column(modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(5.dp)) {
                val stroke = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                drawArc(track, 0f, 360f, useCenter = false, style = stroke)
                drawArc(color, -90f, 360f * ((score ?: 0) / 100f), useCenter = false, style = stroke)
            }
            Text(score?.let { "$it" } ?: "–", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.labelMedium, color = Labs.colors.inkMuted)
    }
}

@Composable
private fun VitalRow(label: String, value: Double?, baseline: Double?, unit: String, decimals: Int = 0) {
    val text = value?.let { "${it.decimalFormatted(decimals)} $unit" } ?: "–"
    val base = baseline?.let { " · Ø ${it.decimalFormatted(decimals)}" }.orEmpty()
    LabeledRow(label, text + base)
}

@Composable
private fun GoalRow(label: String, value: Double, goal: Double, text: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(label, modifier = Modifier.weight(1f))
            Text(text, fontWeight = FontWeight.SemiBold)
        }
        GoalBar(value, goal)
    }
}

/** The 14 days up to the shown day, a bar per day, tapping one shows that day. */
@Composable
private fun TrendBars(
    title: String,
    overview: HealthOverviewDto,
    value: (DayScoreDto) -> Double?,
    format: (Double) -> String,
    max: Double?,
    onSelect: (LocalDate) -> Unit,
) {
    val values = overview.history.map(value)
    val top = max ?: (values.filterNotNull().maxOrNull() ?: 0.0)
    val selected = overview.history.firstOrNull { it.date == overview.date }?.let(value)

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row {
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(selected?.let(format) ?: "–", color = Labs.colors.inkMuted)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
            overview.history.forEachIndexed { index, day ->
                val v = values[index]
                val fraction = if (v != null && top > 0) (v / top).coerceIn(0.04, 1.0).toFloat() else 0.04f
                val isSelected = day.date == overview.date
                Box(
                    Modifier.weight(1f).fillMaxHeight(fraction).clip(RoundedCornerShape(3.dp))
                        .background(if (v == null) Labs.colors.track else if (isSelected) Labs.colors.brandStrong else Labs.colors.brand)
                        .clickable { onSelect(day.date) },
                )
            }
        }
    }
}
