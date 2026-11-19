package tech.project101.labs.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import tech.project101.labs.core.formatDuration
import tech.project101.labs.core.userMessage
import tech.project101.labs.labsViewModel
import tech.project101.labs.ui.components.ConfirmDialog
import tech.project101.labs.ui.components.EmptyState
import tech.project101.labs.ui.components.ErrorDialog
import tech.project101.labs.ui.components.LabeledRow
import tech.project101.labs.ui.components.LabsSection
import tech.project101.labs.ui.components.Loading
import tech.project101.labs.ui.theme.Labs
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class WorkoutDetailViewModel(private val id: Long, private val service: WorkoutService) : ViewModel() {
    var session by mutableStateOf<WorkoutSession?>(null)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var actionError by mutableStateOf<String?>(null)

    init {
        load()
    }

    fun load() {
        loadError = null
        viewModelScope.launch {
            try {
                session = service.session(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadError = e.userMessage
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                service.delete(id)
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                actionError = e.userMessage
            }
        }
    }
}

/** The done sets of a finished workout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(id: Long, onBack: () -> Unit) {
    val model = labsViewModel(key = "detail-$id") { WorkoutDetailViewModel(id, it.workoutService) }
    var confirmDelete by remember { mutableStateOf(false) }
    val session = model.session

    Scaffold(
        containerColor = Labs.colors.canvas,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Labs.colors.canvas),
                title = { Text(session?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
            )
        },
    ) { padding ->
        val loadError = model.loadError
        when {
            session != null -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    LabsSection {
                        LabeledRow(
                            "Datum",
                            session.startedAt.atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)),
                        )
                        LabeledRow("Dauer", formatDuration(session.durationSeconds()))
                        LabeledRow("Volumen", WorkoutStats.formattedVolume(WorkoutStats.volume(session.doneSets)))
                    }
                }
                items(session.exercises, key = { it.key }) { exercise ->
                    LabsSection(title = exercise.name) {
                        exercise.note?.let { note ->
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.Notes, null, tint = Labs.colors.inkMuted, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(note, color = Labs.colors.inkMuted)
                            }
                        }
                        exercise.cardio?.let { LabeledRow("Cardio", "${it.minutes} min") }
                        exercise.sets.filter { it.done }.forEachIndexed { index, set ->
                            LabeledRow("Satz ${index + 1}", set.formatted)
                        }
                    }
                }
                item {
                    LabsSection {
                        TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Training löschen", color = Labs.colors.danger)
                        }
                    }
                }
            }
            loadError != null -> EmptyState(Icons.Filled.Warning, "Nicht geladen", loadError, "Erneut versuchen", model::load, Modifier.padding(padding))
            else -> Loading(Modifier.padding(padding))
        }
    }

    if (confirmDelete && session != null) {
        ConfirmDialog(
            title = "„${session.name}“ löschen?",
            confirmLabel = "Löschen",
            onConfirm = { model.delete(onBack) },
            onDismiss = { confirmDelete = false },
        )
    }
    ErrorDialog(model.actionError) { model.actionError = null }
}
