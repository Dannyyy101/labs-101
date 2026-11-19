package tech.project101.labs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.project101.labs.ui.theme.Labs

/** A card of rows with an optional header above, the counterpart of a list section on iOS. */
@Composable
fun LabsSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    trailing: String? = null,
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        when {
            header != null -> Box(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp)) { header() }
            title != null -> Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (trailing != null) {
                    Text(trailing, style = MaterialTheme.typography.bodySmall, color = Labs.colors.inkMuted)
                }
            }
        }
        Surface(shape = RoundedCornerShape(20.dp), color = Labs.colors.surfaceRaised, tonalElevation = 0.dp) {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), content = content)
        }
    }
}

/** A thin progress bar on the track, green once the goal is reached. */
@Composable
fun GoalBar(value: Double, goal: Double, color: Color = Labs.colors.brand, modifier: Modifier = Modifier) {
    val fraction = if (goal > 0) (value / goal).coerceIn(0.0, 1.0).toFloat() else 0f
    val fill = if (goal > 0 && value >= goal) Labs.colors.successBar else color
    Box(modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Labs.colors.track)) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(CircleShape).background(fill))
    }
}

/** A glyph in a soft brand tile, [solid] fills it with the brand for the selected or running object. */
@Composable
fun IconTile(icon: ImageVector, solid: Boolean = false, size: Int = 36) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.28).dp))
            .background(if (solid) Labs.colors.brand else Labs.colors.brandSoft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (solid) Labs.colors.onBrand else Labs.colors.brand, modifier = Modifier.size((size * 0.5).dp))
    }
}

/** Shown instead of content: nothing there yet, or loading failed. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    text: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Labs.colors.inkMuted, modifier = Modifier.size(44.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (text != null) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Labs.colors.inkMuted, textAlign = TextAlign.Center)
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun Loading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/** An error of an action, shown until it is dismissed. */
@Composable
fun ErrorDialog(message: String?, onDismiss: () -> Unit) {
    if (message == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        title = { Text("Fehler") },
        text = { Text(message) },
    )
}

/** Asks before something is deleted. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String? = null,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) { Text(confirmLabel, color = Labs.colors.danger) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        title = { Text(title) },
        text = text?.let { { Text(it) } },
    )
}

/** A label on the left and a value on the right, like LabeledContent. */
@Composable
fun LabeledRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, color = Labs.colors.inkMuted)
    }
}
