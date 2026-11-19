package tech.project101.labs.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tech.project101.labs.core.formatDuration
import tech.project101.labs.core.formatHours
import tech.project101.labs.core.userMessage
import tech.project101.labs.labsViewModel
import tech.project101.labs.ui.components.EmptyState
import tech.project101.labs.ui.components.ErrorDialog
import tech.project101.labs.ui.components.IconTile
import tech.project101.labs.ui.components.LabsSection
import tech.project101.labs.ui.components.Loading
import tech.project101.labs.ui.theme.Labs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Templates, the running workout and the history, the data of /workouts in the web. */
class WorkoutOverviewViewModel(private val service: WorkoutService) : ViewModel() {
    var templates by mutableStateOf<List<WorkoutTemplate>>(emptyList())
        private set

    /** finished workouts of the last year, newest first */
    var history by mutableStateOf<List<WorkoutSession>>(emptyList())
        private set
    var active by mutableStateOf<WorkoutSession?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var hasLoaded by mutableStateOf(false)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var actionError by mutableStateOf<String?>(null)

    /** the template being started, 0 for a free workout */
    var starting by mutableStateOf<Long?>(null)
        private set

    fun load() {
        viewModelScope.launch {
            isLoading = true
            try {
                val now = Instant.now()
                val templates = async { service.templates() }
                val history = async { service.sessions(now.minus(365, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS)) }
                val active = async { service.activeSession() }
                this@WorkoutOverviewViewModel.templates = templates.await()
                this@WorkoutOverviewViewModel.history = history.await()
                this@WorkoutOverviewViewModel.active = active.await()
                loadError = null
                hasLoaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadError = e.userMessage
            } finally {
                isLoading = false
            }
        }
    }

    /** Starts the workout and opens it, or opens the running one when there already is one. */
    fun start(templateId: Long?, onOpen: (Long) -> Unit) {
        starting = templateId ?: 0
        viewModelScope.launch {
            try {
                val session = service.start(templateId)
                active = session
                onOpen(session.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!e.isAnotherWorkoutRunning) {
                    actionError = e.userMessage
                    return@launch
                }
                // another workout is still running, open that one instead
                active = runCatching { service.activeSession() }.getOrNull()
                active?.let { onOpen(it.id) }
            } finally {
                starting = null
            }
        }
    }

    fun lastSession(template: WorkoutTemplate): WorkoutSession? = history.firstOrNull { it.workoutId == template.id }

    val recent: List<WorkoutSession>
        get() {
            val from = Instant.now().minus(30, ChronoUnit.DAYS)
            return history.filter { it.startedAt >= from }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(modifier: Modifier, onOpenSession: (Long) -> Unit, onOpenDetail: (Long) -> Unit) {
    val model = labsViewModel { WorkoutOverviewViewModel(it.workoutService) }

    // also when coming back from a workout
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.load() }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        containerColor = Labs.colors.canvas,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Labs.colors.canvas),
                title = { Text("Training", fontWeight = FontWeight.Bold) },
            )
        },
    ) { padding ->
        val loadError = model.loadError
        when {
            loadError != null && !model.hasLoaded -> EmptyState(
                Icons.Filled.WifiOff, "Keine Verbindung", loadError, "Erneut versuchen", model::load, Modifier.padding(padding),
            )
            !model.hasLoaded -> Loading(Modifier.padding(padding))
            else -> PullToRefreshBox(model.isLoading, model::load, Modifier.padding(padding)) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    model.active?.let { active ->
                        item { ActiveWorkoutBanner(active) { onOpenSession(active.id) } }
                    }
                    item {
                        LabsSection(title = "Letzte 30 Tage") { RecentStats(model.recent) }
                    }
                    item {
                        LabsSection(title = "Vorlagen") {
                            val blocked = model.active != null || model.starting != null
                            model.templates.forEach { template ->
                                TemplateRow(
                                    template = template,
                                    last = model.lastSession(template),
                                    isStarting = model.starting == template.id,
                                    enabled = !blocked,
                                    onStart = { model.start(template.id, onOpenSession) },
                                )
                                HorizontalDivider(Modifier.padding(start = 16.dp), color = Labs.colors.line)
                            }
                            Row(
                                Modifier.fillMaxWidth().clickable(enabled = !blocked) { model.start(null, onOpenSession) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val tint = if (blocked) Labs.colors.inkMuted else Labs.colors.brand
                                Icon(Icons.Filled.PlayArrow, null, tint = tint)
                                Spacer(Modifier.width(10.dp))
                                Text(if (model.starting == 0L) "Startet…" else "Freies Training", color = tint)
                            }
                        }
                    }
                    item {
                        LabsSection(title = "Trainings") {
                            if (model.history.isEmpty()) {
                                Text(
                                    "Noch keine Trainings im letzten Jahr",
                                    color = Labs.colors.inkMuted,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                            model.history.forEachIndexed { index, session ->
                                SessionRow(session) { onOpenDetail(session.id) }
                                if (index < model.history.lastIndex) {
                                    HorizontalDivider(Modifier.padding(start = 16.dp), color = Labs.colors.line)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    ErrorDialog(model.actionError) { model.actionError = null }
}

/** Seconds since [start], counting up every second. */
@Composable
fun rememberElapsedSeconds(start: Instant): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(start) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    return ((now - start.toEpochMilli()) / 1000).coerceAtLeast(0)
}

@Composable
private fun ActiveWorkoutBanner(session: WorkoutSession, onOpen: () -> Unit) {
    val elapsed = rememberElapsedSeconds(session.startedAt)
    // the running object is filled with the brand, like a solid icon tile on the web
    Surface(color = Labs.colors.brand, shape = MaterialTheme.shapes.extraLarge, onClick = onOpen) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.FitnessCenter, null, tint = Labs.colors.onBrand, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Training läuft", style = MaterialTheme.typography.bodySmall, color = Labs.colors.onBrand)
                Text(session.name, style = MaterialTheme.typography.titleMedium, color = Labs.colors.onBrand, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatDuration(elapsed), style = MaterialTheme.typography.titleMedium, color = Labs.colors.onBrand, fontWeight = FontWeight.Bold)
                Text(
                    "${session.doneSets.size} / ${session.allSets.size} Sätze",
                    style = MaterialTheme.typography.bodySmall,
                    color = Labs.colors.onBrand,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Labs.colors.onBrand)
        }
    }
}

@Composable
private fun RecentStats(sessions: List<WorkoutSession>) {
    val sets = sessions.flatMap { it.doneSets }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row {
            Stat("Trainings", "${sessions.size}", Icons.Filled.FitnessCenter, Modifier.weight(1f))
            Stat("Zeit", formatHours(sessions.sumOf { it.durationSeconds() }), Icons.Filled.AccessTime, Modifier.weight(1f))
        }
        Row {
            Stat("Volumen", WorkoutStats.formattedVolume(WorkoutStats.volume(sets)), Icons.Filled.Scale, Modifier.weight(1f))
            Stat("Sätze", "${sets.size}", Icons.Filled.Checklist, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(title: String, value: String, icon: ImageVector, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium, color = Labs.colors.inkMuted)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun TemplateRow(
    template: WorkoutTemplate,
    last: WorkoutSession?,
    isStarting: Boolean,
    enabled: Boolean,
    onStart: () -> Unit,
) {
    fun count(value: Int, one: String, many: String) = "$value ${if (value == 1) one else many}"
    val summary = buildList {
        add(count(template.items.size, "Übung", "Übungen"))
        add(count(template.setCount, "Satz", "Sätze"))
        if (template.cardioMinutes > 0) add("${template.cardioMinutes} min Cardio")
    }.joinToString(" · ")

    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(summary, style = MaterialTheme.typography.bodyMedium, color = Labs.colors.inkMuted)
            if (template.sharedWithMe) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Group, null, tint = Labs.colors.brandStrong, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Geteilt von ${template.ownerName ?: "einem Freund"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Labs.colors.brandStrong,
                    )
                }
            }
            Text(
                last?.let { "Zuletzt ${it.startedAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d. MMM"))}" }
                    ?: "Noch nie trainiert",
                style = MaterialTheme.typography.bodySmall,
                color = Labs.colors.inkMuted,
            )
        }
        Button(onClick = onStart, enabled = enabled, modifier = Modifier.widthIn(min = 112.dp)) {
            Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (isStarting) "Startet…" else "Starten", maxLines = 1)
        }
    }
}

@Composable
private fun SessionRow(session: WorkoutSession, onClick: () -> Unit) {
    val sets = session.doneSets
    val date = session.startedAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EE, d. MMM, HH:mm"))
    val cardio = if (session.cardioMinutes > 0) " · ${session.cardioMinutes} min Cardio" else ""

    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(session.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("$date · ${formatDuration(session.durationSeconds())}", style = MaterialTheme.typography.bodyMedium, color = Labs.colors.inkMuted)
            Text(
                "${sets.size} Sätze · ${WorkoutStats.formattedVolume(WorkoutStats.volume(sets))}$cardio",
                style = MaterialTheme.typography.bodySmall,
                color = Labs.colors.inkMuted,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Labs.colors.inkMuted)
    }
}
