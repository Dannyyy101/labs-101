package tech.project101.labs.workout

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import tech.project101.labs.core.compactFormatted
import tech.project101.labs.core.formatDuration
import tech.project101.labs.core.inputFormatted
import tech.project101.labs.core.parseDecimal
import tech.project101.labs.labsViewModel
import tech.project101.labs.ui.components.ConfirmDialog
import tech.project101.labs.ui.components.EmptyState
import tech.project101.labs.ui.components.ErrorDialog
import tech.project101.labs.ui.components.Loading
import tech.project101.labs.ui.theme.Labs

/** The running workout: sets are checked off while training, the Android counterpart of /workouts/session. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(id: Long, onClose: () -> Unit) {
    val context = LocalContext.current
    val model = labsViewModel(key = "session-$id") {
        WorkoutSessionViewModel(id, it.workoutService, context.getSharedPreferences("workout", Context.MODE_PRIVATE))
    }
    val haptics = LocalHapticFeedback.current
    val focus = LocalFocusManager.current

    var picker by remember { mutableStateOf<Boolean?>(null) } // false: strength, true: cardio
    var showFinish by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }

    fun close() {
        model.saveNow()
        onClose()
    }

    BackHandler { close() }
    // the app may be ended in the background, so a pending change is sent right away
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { model.saveNow() }
    LaunchedEffect(model.restsFinished) {
        if (model.restsFinished > 0) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Scaffold(
        containerColor = Labs.colors.canvas,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Labs.colors.canvas),
                navigationIcon = { IconButton(onClick = ::close) { Icon(Icons.Filled.KeyboardArrowDown, "Schließen") } },
                title = {
                    Row(Modifier.clickable { renaming = true }, verticalAlignment = Alignment.CenterVertically) {
                        Text(model.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.Edit, "Umbenennen", tint = Labs.colors.inkMuted, modifier = Modifier.size(16.dp))
                    }
                },
                actions = {
                    // only a failed save is worth a hint, the rest happens quietly
                    if (model.saveState == WorkoutSessionViewModel.SaveState.FAILED) {
                        TextButton(onClick = model::retrySave) {
                            Text("Nicht gespeichert – erneut versuchen", color = Labs.colors.danger, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
            )
        },
        bottomBar = {
            val session = model.session
            if (session != null) BottomBar(model, session)
        },
    ) { padding ->
        val session = model.session
        val loadError = model.loadError
        when {
            session != null -> LazyColumn(
                Modifier.fillMaxSize().padding(padding).imePadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(model.exercises, key = { it.key }) { item ->
                    if (item.cardio != null) {
                        CardioCard(model, item)
                    } else {
                        ExerciseCard(model, item)
                    }
                }
                if (model.exercises.isEmpty()) {
                    item {
                        EmptyState(Icons.Filled.FitnessCenter, "Leeres Training", "Füge die erste Übung hinzu, um Sätze zu erfassen.")
                    }
                }
                item {
                    Surface(color = Labs.colors.surfaceRaised, shape = RoundedCornerShape(20.dp)) {
                        Column {
                            ActionRow(Icons.Filled.Add, "Übung hinzufügen") { picker = false }
                            HorizontalDivider(Modifier.padding(start = 16.dp), color = Labs.colors.line)
                            ActionRow(Icons.Filled.Favorite, "Cardio hinzufügen") { picker = true }
                            HorizontalDivider(Modifier.padding(start = 16.dp), color = Labs.colors.line)
                            ActionRow(Icons.Filled.Delete, "Training verwerfen", Labs.colors.danger) { confirmDiscard = true }
                        }
                    }
                }
                item {
                    Button(
                        onClick = { focus.clearFocus(); showFinish = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text("Training beenden", fontWeight = FontWeight.SemiBold) }
                }
            }
            loadError != null -> EmptyState(Icons.Filled.Warning, "Nicht geladen", loadError, "Erneut versuchen", model::load, Modifier.padding(padding))
            else -> Loading(Modifier.padding(padding))
        }
    }

    picker?.let { cardio ->
        ExercisePickerSheet(
            exercises = model.catalog.filter { it.isCardio == cardio },
            title = if (cardio) "Cardio hinzufügen" else "Übung hinzufügen",
            onPick = { model.add(it) },
            onDismiss = { picker = null },
        )
    }
    if (showFinish) {
        FinishSheet(
            model,
            onFinish = { model.finish { showFinish = false; onClose() } },
            onDiscard = { model.discard { showFinish = false; onClose() } },
            onDismiss = { showFinish = false },
        )
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = "Training verwerfen?",
            text = "Alle erfassten Sätze gehen verloren.",
            confirmLabel = "Verwerfen",
            onConfirm = { model.discard(onClose) },
            onDismiss = { confirmDiscard = false },
        )
    }
    if (renaming) RenameDialog(model.name, onRename = model::rename, onDismiss = { renaming = false })
    ErrorDialog(model.actionError) { model.actionError = null }
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: androidx.compose.ui.graphics.Color = Labs.colors.brand, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint)
        Spacer(Modifier.width(12.dp))
        Text(label, color = tint)
    }
}

// exercises

@Composable
private fun ExerciseCard(model: WorkoutSessionViewModel, item: SessionExercise) {
    val previous = WorkoutStats.lastSets(item.exerciseId, model.history)
    val subtitle = when {
        previous != null -> "Letztes Mal: " + previous.joinToString(", ") { it.formatted }
        else -> model.exercise(item.exerciseId)?.muscles?.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Zum ersten Mal"
    }
    var confirmRemove by remember { mutableStateOf(false) }

    Surface(color = Labs.colors.surfaceRaised, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(vertical = 8.dp)) {
            CardHeader(
                name = item.name,
                done = item.sets.isNotEmpty() && item.sets.all { it.done },
                subtitle = subtitle,
                onRemove = { if (item.sets.any { it.done }) confirmRemove = true else model.removeExercise(item.key) },
            )
            NoteField(item.note) { text -> model.updateExercise(item.key) { it.copy(note = text.ifEmpty { null }?.take(500)) } }

            item.sets.forEachIndexed { index, set ->
                SetRow(
                    number = index + 1,
                    set = set,
                    previous = previous?.getOrNull(index),
                    onChange = { transform -> model.updateSet(item.key, set.key, transform) },
                    onToggle = { model.toggle(item.key, set.key) },
                    onRemove = { model.removeSet(item.key, set.key) },
                )
            }
            TextButton(onClick = { model.addSet(item.key) }, modifier = Modifier.padding(start = 4.dp)) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Satz hinzufügen")
            }
        }
    }

    if (confirmRemove) {
        ConfirmDialog(
            title = "${item.name} mit allen Sätzen entfernen?",
            confirmLabel = "Entfernen",
            onConfirm = { model.removeExercise(item.key) },
            onDismiss = { confirmRemove = false },
        )
    }
}

@Composable
private fun CardHeader(name: String, done: Boolean, subtitle: String, onRemove: () -> Unit) {
    Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (done) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.CheckCircle, null, tint = Labs.colors.success, modifier = Modifier.size(18.dp))
                }
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Labs.colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, "Entfernen", tint = Labs.colors.danger) }
    }
}

@Composable
private fun NoteField(note: String?, onChange: (String) -> Unit) {
    var text by remember { mutableStateOf(note.orEmpty()) }
    BasicTextField(
        value = text,
        onValueChange = { text = it; onChange(it) },
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Labs.colors.ink),
        maxLines = 4,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (text.isEmpty()) Labs.colors.surfaceSunken else Labs.colors.warningSoft)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        decorationBox = { inner ->
            if (text.isEmpty()) Text("Notiz, z. B. Sitz auf Stufe 4", style = MaterialTheme.typography.bodyMedium, color = Labs.colors.inkMuted)
            inner()
        },
    )
}

@Composable
private fun SetRow(
    number: Int,
    set: SessionSet,
    previous: SessionSet?,
    onChange: ((SessionSet) -> SessionSet) -> Unit,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    val focus = LocalFocusManager.current
    var menu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().background(if (set.done) Labs.colors.successSoft else Labs.colors.surfaceRaised)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Text(
                    "$number",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (set.done) Labs.colors.success else Labs.colors.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(28.dp).clickable { menu = true },
                )
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Satz entfernen") }, onClick = { menu = false; onRemove() })
                }
            }
            // tapping last time takes it over
            Text(
                previous?.let { "${it.weightKg.compactFormatted} × ${it.reps}" } ?: "–",
                style = MaterialTheme.typography.bodySmall,
                color = Labs.colors.inkMuted,
                maxLines = 1,
                modifier = Modifier.weight(1f).clickable(enabled = previous != null) {
                    previous?.let { p -> onChange { it.copy(weightKg = p.weightKg, reps = p.reps) } }
                }.padding(horizontal = 6.dp, vertical = 8.dp),
            )
            NumberField(set.weightKg, "kg", decimals = true) { value -> onChange { it.copy(weightKg = value) } }
            Spacer(Modifier.width(6.dp))
            NumberField(set.reps.toDouble(), "Wdh", decimals = false) { value -> onChange { it.copy(reps = value.toInt()) } }
            Spacer(Modifier.width(8.dp))
            DoneButton(set.done) {
                focus.clearFocus()
                onToggle()
            }
        }
        RestPicker(set.rest) { seconds -> onChange { it.copy(restSeconds = seconds) } }
    }
}

@Composable
private fun DoneButton(done: Boolean, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Box(
        Modifier.size(width = 44.dp, height = 36.dp).clip(RoundedCornerShape(10.dp))
            .background(if (done) Labs.colors.successBar else Labs.colors.surfaceSunken)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Check, if (done) "Nicht erledigt" else "Erledigt", tint = if (done) Labs.colors.onBrand else Labs.colors.inkMuted)
    }
}

/** the rest after the set, shown between the sets */
@Composable
private fun RestPicker(rest: Int, onChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = if (rest in WorkoutStats.REST_OPTIONS) WorkoutStats.REST_OPTIONS else (WorkoutStats.REST_OPTIONS + rest).sorted()
    val tint = if (rest > 0) Labs.colors.warning else Labs.colors.inkMuted

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = Labs.colors.line)
            Row(
                Modifier.clip(RoundedCornerShape(50)).clickable { expanded = true }.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Timer, null, tint = tint, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(WorkoutStats.formattedRest(rest), style = MaterialTheme.typography.labelMedium, color = tint, fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider(Modifier.weight(1f), color = Labs.colors.line)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { seconds ->
                DropdownMenuItem(
                    text = { Text(WorkoutStats.formattedRest(seconds), fontWeight = if (seconds == rest) FontWeight.Bold else null) },
                    onClick = { expanded = false; onChange(seconds) },
                )
            }
        }
    }
}

/**
 * A number typed as text: the value is selected on focus to type over it and every key changes the
 * value right away. Changes from elsewhere (last time taken over) show up while it isn't focused.
 */
@Composable
private fun NumberField(value: Double, unit: String, decimals: Boolean, onChange: (Double) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    var field by remember { mutableStateOf(TextFieldValue(value.inputFormatted)) }
    LaunchedEffect(value, focused) {
        if (!focused && field.text.parseDecimal() != value) field = TextFieldValue(value.inputFormatted)
    }

    Row(
        Modifier.width(80.dp).clip(RoundedCornerShape(8.dp)).background(Labs.colors.surfaceSunken)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = field,
            onValueChange = { new ->
                val filtered = new.text.filter { it.isDigit() || (decimals && (it == ',' || it == '.')) }
                field = new.copy(text = filtered)
                onChange(filtered.parseDecimal() ?: 0.0)
            },
            singleLine = true,
            textStyle = TextStyle(color = Labs.colors.ink, textAlign = TextAlign.End, fontWeight = FontWeight.Medium),
            keyboardOptions = KeyboardOptions(keyboardType = if (decimals) KeyboardType.Decimal else KeyboardType.Number),
            modifier = Modifier.weight(1f).onFocusChanged { state ->
                if (state.isFocused && !focused) field = field.copy(selection = TextRange(0, field.text.length))
                if (!state.isFocused && focused) field = TextFieldValue(value.inputFormatted)
                focused = state.isFocused
            },
        )
        Spacer(Modifier.width(3.dp))
        Text(unit, style = MaterialTheme.typography.labelSmall, color = Labs.colors.inkMuted)
    }
}

/** Cardio like running or rowing: only its minutes, checked off like a set. */
@Composable
private fun CardioCard(model: WorkoutSessionViewModel, item: SessionExercise) {
    val cardio = item.cardio ?: return
    val previous = WorkoutStats.lastCardioMinutes(item.exerciseId, model.history)
    var confirmRemove by remember { mutableStateOf(false) }

    Surface(color = Labs.colors.surfaceRaised, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(vertical = 8.dp)) {
            CardHeader(
                name = item.name,
                done = cardio.done,
                subtitle = previous?.let { "Letztes Mal: $it min" } ?: "Cardio",
                onRemove = { if (cardio.done) confirmRemove = true else model.removeExercise(item.key) },
            )
            Row(
                Modifier.fillMaxWidth().background(if (cardio.done) Labs.colors.successSoft else Labs.colors.surfaceRaised)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Favorite, null, tint = Labs.colors.brand)
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = {
                    model.updateExercise(item.key) { it.copy(cardio = cardio.copy(minutes = (cardio.minutes - 5).coerceAtLeast(0))) }
                }) { Text("−5") }
                Spacer(Modifier.width(6.dp))
                FilledTonalButton(onClick = {
                    model.updateExercise(item.key) { it.copy(cardio = cardio.copy(minutes = (cardio.minutes + 5).coerceAtMost(600))) }
                }) { Text("+5") }
                Spacer(Modifier.weight(1f))
                NumberField(cardio.minutes.toDouble(), "min", decimals = false) { value ->
                    model.updateExercise(item.key) { it.copy(cardio = cardio.copy(minutes = value.toInt().coerceAtLeast(0))) }
                }
                Spacer(Modifier.width(8.dp))
                DoneButton(cardio.done) {
                    model.updateExercise(item.key) { it.copy(cardio = cardio.copy(done = !cardio.done)) }
                }
            }
        }
    }

    if (confirmRemove) {
        ConfirmDialog(
            title = "${item.name} entfernen?",
            confirmLabel = "Entfernen",
            onConfirm = { model.removeExercise(item.key) },
            onDismiss = { confirmRemove = false },
        )
    }
}

// bars and sheets

@Composable
private fun BottomBar(model: WorkoutSessionViewModel, session: WorkoutSession) {
    val elapsed = rememberElapsedSeconds(session.startedAt)
    Surface(color = Labs.colors.surfaceRaised, shadowElevation = 8.dp) {
        Column(Modifier.navigationBarsPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            model.rest?.let { rest -> RestBar(rest, model::adjustRest, model::endRest) }
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(formatDuration(elapsed), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${model.doneSets.size} / ${model.allSets.size} Sätze", color = Labs.colors.inkMuted)
            }
        }
    }
}

@Composable
private fun RestBar(rest: WorkoutSessionViewModel.Rest, onAdjust: (Long) -> Unit, onEnd: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(rest) {
        while (true) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    val leftMillis = (rest.untilMillis - now).coerceAtLeast(0)
    val running = leftMillis > 0
    val fraction = if (running && rest.totalSeconds > 0) (leftMillis / 1000f / rest.totalSeconds).coerceIn(0f, 1f) else 1f

    Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(Labs.colors.surfaceSunken)) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(if (running) Labs.colors.brandSoft else Labs.colors.successSoft))
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (running) {
                Text(
                    "Pause ${formatDuration((leftMillis + 999) / 1000)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Labs.colors.brandStrong,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onAdjust(-15) }) { Text("−15") }
                TextButton(onClick = { onAdjust(15) }) { Text("+15") }
            } else {
                Text(
                    "Pause vorbei – nächster Satz!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Labs.colors.success,
                    modifier = Modifier.weight(1f),
                )
            }
            IconButton(onClick = onEnd) { Icon(Icons.Filled.Close, "Pause beenden") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinishSheet(model: WorkoutSessionViewModel, onFinish: () -> Unit, onDiscard: () -> Unit, onDismiss: () -> Unit) {
    val session = model.session ?: return
    val done = model.doneSets
    val cardio = model.exercises.mapNotNull { it.cardio }
    val cardioDone = cardio.filter { it.done && it.minutes > 0 }
    val open = model.allSets.size - done.size + cardio.size - cardioDone.size
    // done cardio alone is worth saving too
    val nothingDone = done.isEmpty() && cardioDone.isEmpty()
    val message = when {
        done.size + cardioDone.size == 0 -> "Hake erledigte Sätze ab, damit sie gespeichert werden – oder verwirf das Training."
        open == 0 -> "Starke Leistung, alles erledigt!"
        open == 1 -> "1 Eintrag ist nicht abgehakt und wird nicht gespeichert."
        else -> "$open Einträge sind nicht abgehakt und werden nicht gespeichert."
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                if (nothingDone) "Noch kein Satz erledigt" else "Training beenden?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(message, color = Labs.colors.inkMuted)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Labs.colors.surfaceSunken).padding(16.dp)) {
                SheetStat("Zeit", formatDuration(session.durationSeconds()), Modifier.weight(1f))
                SheetStat("Sätze", "${done.size}", Modifier.weight(1f))
                if (done.isEmpty() && cardioDone.isNotEmpty()) {
                    SheetStat("Cardio", "${cardioDone.sumOf { it.minutes }} min", Modifier.weight(1f))
                } else {
                    SheetStat("Volumen", WorkoutStats.formattedVolume(WorkoutStats.volume(done)), Modifier.weight(1f))
                }
            }
            if (nothingDone) {
                Button(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) { Text("Training verwerfen") }
            } else {
                Button(onClick = onFinish, enabled = !model.isFinishing, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(if (model.isFinishing) "Speichert…" else "Training beenden")
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Weiter trainieren") }
        }
    }
}

@Composable
private fun SheetStat(title: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = Labs.colors.inkMuted)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Picks an exercise for the running workout, searchable by name and muscle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(exercises: List<Exercise>, title: String, onPick: (Exercise) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val trimmed = query.trim()
    val filtered = exercises.sortedBy { it.name.lowercase() }.filter { exercise ->
        trimmed.isEmpty() || exercise.name.contains(trimmed, ignoreCase = true) ||
            exercise.muscles.any { it.contains(trimmed, ignoreCase = true) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(0.9f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Übung suchen…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (filtered.isEmpty()) {
                EmptyState(Icons.Filled.Search, "Keine Übung gefunden")
            }
            LazyColumn(Modifier.imePadding()) {
                items(filtered, key = { it.id }) { exercise ->
                    Column(
                        Modifier.fillMaxWidth().clickable { onPick(exercise); onDismiss() }.padding(horizontal = 24.dp, vertical = 12.dp),
                    ) {
                        Text(exercise.name)
                        if (exercise.muscles.isNotEmpty()) {
                            Text(exercise.muscles.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = Labs.colors.inkMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameDialog(name: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Training umbenennen") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = { onRename(text); onDismiss() }, enabled = text.isNotBlank()) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
