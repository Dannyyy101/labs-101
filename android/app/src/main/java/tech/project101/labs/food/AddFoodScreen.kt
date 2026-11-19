package tech.project101.labs.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import tech.project101.labs.core.compactFormatted
import tech.project101.labs.core.roundedFormatted
import tech.project101.labs.labsViewModel
import tech.project101.labs.ui.FoodEntryRoute
import tech.project101.labs.ui.components.EmptyState
import tech.project101.labs.ui.components.ErrorDialog
import tech.project101.labs.ui.components.LabsSection
import tech.project101.labs.ui.theme.Labs

private enum class InputMode { SEARCH, TEXT }

/** Finds food by name, barcode or free text and tracks it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    initialMeal: Meal,
    onBack: () -> Unit,
    onSelect: (FoodEntryRoute) -> Unit,
    onTracked: () -> Unit,
) {
    var meal by rememberSaveable { mutableStateOf(initialMeal) }
    var mode by rememberSaveable { mutableStateOf(InputMode.SEARCH) }

    Scaffold(
        containerColor = Labs.colors.canvas,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Labs.colors.canvas),
                title = { Text("Essen hinzufügen") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.Close, "Schließen") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).imePadding().fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                InputMode.entries.forEachIndexed { index, item ->
                    SegmentedButton(
                        selected = mode == item,
                        onClick = { mode = item },
                        shape = SegmentedButtonDefaults.itemShape(index, InputMode.entries.size),
                    ) { Text(if (item == InputMode.SEARCH) "Suchen" else "Per Text") }
                }
            }
            MealPicker(meal, { meal = it }, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            when (mode) {
                InputMode.SEARCH -> FoodSearch(meal, onSelect)
                InputMode.TEXT -> TextFoodInput(meal, onTracked)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPicker(meal: Meal, onChange: (Meal) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = meal.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Mahlzeit") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Meal.entries.forEach { option ->
                DropdownMenuItem(text = { Text(option.label) }, onClick = { onChange(option); expanded = false })
            }
        }
    }
}

@Composable
private fun FoodSearch(meal: Meal, onSelect: (FoodEntryRoute) -> Unit) {
    val model = labsViewModel { FoodSearchViewModel(it.foodService) }
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // load the next page shortly before the end of the list
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            model.results.isNotEmpty() && last >= model.results.size - 3
        }
    }
    LaunchedEffect(nearEnd) { if (nearEnd) model.loadNextPage() }

    fun scan() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_E, Barcode.FORMAT_UPC_A)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { barcode ->
                val code = barcode.rawValue ?: return@addOnSuccessListener
                model.lookUp(code) { food -> onSelect(FoodEntryRoute(meal = meal.name, foodId = food.id)) }
            }
            .addOnFailureListener { model.error = "Der Scanner ist auf diesem Gerät nicht verfügbar" }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = model.query,
                onValueChange = model::updateQuery,
                placeholder = { Text("Suchen…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (model.query.isNotEmpty()) {
                        IconButton(onClick = { model.updateQuery("") }) { Icon(Icons.Filled.Clear, "Leeren") }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = ::scan) { Icon(Icons.Filled.QrCodeScanner, "Barcode scannen", tint = Labs.colors.brand) }
        }
        if (model.isLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
        }

        when {
            model.trimmedQuery.isEmpty() && model.results.isEmpty() -> EmptyState(
                Icons.Filled.Search,
                "Lebensmittel suchen",
                "Suche nach dem Namen oder scanne den Barcode einer Verpackung.",
            )
            model.results.isEmpty() && !model.isLoading -> EmptyState(
                Icons.Filled.Search, "Keine Ergebnisse für „${model.trimmedQuery}“",
            )
            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(model.results, key = { _, result -> result.key }) { index, result ->
                    // the rows together look like one card
                    val shape = RoundedCornerShape(
                        topStart = if (index == 0) 20.dp else 0.dp,
                        topEnd = if (index == 0) 20.dp else 0.dp,
                        bottomStart = if (index == model.results.lastIndex) 20.dp else 0.dp,
                        bottomEnd = if (index == model.results.lastIndex) 20.dp else 0.dp,
                    )
                    Surface(color = Labs.colors.surfaceRaised, shape = shape) {
                        Column {
                            SearchRow(result) {
                                val foodId = result.foodId
                                val openFoodId = result.openFoodId
                                // open foods only get copied into our foods when they are tracked
                                when {
                                    foodId != null -> onSelect(FoodEntryRoute(meal = meal.name, foodId = foodId))
                                    openFoodId != null -> onSelect(FoodEntryRoute(meal = meal.name, openFoodId = openFoodId))
                                }
                            }
                            if (index < model.results.lastIndex) {
                                HorizontalDivider(Modifier.padding(start = 16.dp), color = Labs.colors.line)
                            }
                        }
                    }
                }
            }
        }
    }

    ErrorDialog(model.error) { model.error = null }
}

@Composable
private fun SearchRow(result: FoodSearchResult, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(result.name)
        if (result.openFoodId != null) {
            Text("Open Food Facts", style = MaterialTheme.typography.bodySmall, color = Labs.colors.inkMuted)
        }
    }
}

@Composable
private fun TextFoodInput(meal: Meal, onTracked: () -> Unit) {
    val model = labsViewModel { TextFoodViewModel(it.foodService) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                LabsSection {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = model.text,
                            onValueChange = { model.text = it },
                            placeholder = { Text("z. B. 2 Eier, eine Tasse Cappuccino, 1 Banane und 30 g Mandeln") },
                            minLines = 3,
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "Menge · Einheit · Lebensmittel, getrennt durch Komma oder „und“",
                            style = MaterialTheme.typography.bodySmall,
                            color = Labs.colors.inkMuted,
                        )
                        OutlinedButton(onClick = model::extract, enabled = model.canExtract) {
                            if (model.isExtracting) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Erkennen")
                        }
                    }
                }
            }
            if (model.items.isNotEmpty()) {
                item {
                    LabsSection(title = "Vorschau") {
                        model.items.forEachIndexed { index, item ->
                            PreviewRow(
                                item = item,
                                isSavingUnit = item.id in model.savingUnitIds,
                                onChangeUnitGrams = { grams -> model.update(item.id) { it.copy(unitGrams = grams.coerceAtLeast(0.0)) } },
                                onSaveUnit = { model.saveUnit(item) },
                                onRemove = { model.remove(item) },
                            )
                            if (index < model.items.lastIndex) HorizontalDivider(color = Labs.colors.line)
                        }
                    }
                }
            }
        }

        if (model.items.isNotEmpty()) {
            Surface(color = Labs.colors.surfaceRaised, shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        val hint = when {
                            model.notFound > 0 -> "${model.notFound} nicht gefunden – bitte entfernen"
                            model.pendingUnits > 0 -> "${model.pendingUnits} neue Einheit(en) bestätigen"
                            else -> "${model.items.size} Lebensmittel"
                        }
                        Text(hint, style = MaterialTheme.typography.bodySmall, color = Labs.colors.inkMuted)
                        Text("${model.totalKcal.roundedFormatted} kcal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { model.trackAll(meal, onTracked) }, enabled = model.canSubmit) {
                        if (model.isSubmitting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Eintragen")
                    }
                }
            }
        }
    }

    ErrorDialog(model.error) { model.error = null }
}

@Composable
private fun PreviewRow(
    item: PreviewItem,
    isSavingUnit: Boolean,
    onChangeUnitGrams: (Double) -> Unit,
    onSaveUnit: () -> Unit,
    onRemove: () -> Unit,
) {
    val approx = if (item.status == PreviewItem.Status.NEW_UNIT) "≈ " else ""
    val subtitle = buildList {
        when {
            item.status == PreviewItem.Status.NOT_FOUND -> add("Nicht gefunden")
            item.isGramUnit -> add("${item.amount.compactFormatted} ${item.unit}")
            else -> add("${item.amount.compactFormatted} ${item.unit} · $approx${item.grams.roundedFormatted} g")
        }
        if (item.openFoodId != null) add("Open Food Facts")
    }.joinToString(" · ")

    Column(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (item.status) {
                PreviewItem.Status.OK -> Icon(Icons.Filled.CheckCircle, "Erkannt", tint = Labs.colors.success)
                PreviewItem.Status.NEW_UNIT -> Icon(Icons.AutoMirrored.Filled.Help, "Neue Einheit", tint = Labs.colors.warning)
                PreviewItem.Status.NOT_FOUND -> Icon(Icons.Filled.Cancel, "Nicht gefunden", tint = Labs.colors.danger)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.food?.name ?: item.query, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Labs.colors.inkMuted)
            }
            if (item.food != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("$approx${item.kcal.roundedFormatted}", fontWeight = FontWeight.SemiBold)
                    Text("kcal", style = MaterialTheme.typography.bodySmall, color = Labs.colors.inkMuted)
                }
            }
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, "Entfernen", tint = Labs.colors.inkMuted) }
        }

        if (item.status == PreviewItem.Status.NEW_UNIT) {
            NewUnitEditor(item, isSavingUnit, onChangeUnitGrams, onSaveUnit)
        }
    }
}

/** Asks how many grams one unit of the food has, e.g. 1 Tasse = 200 g. */
@Composable
private fun NewUnitEditor(item: PreviewItem, isSaving: Boolean, onChangeGrams: (Double) -> Unit, onSave: () -> Unit) {
    val guess = PreviewItem.guessedGrams(item.unit)
    val presets = listOf(guess - 50, guess, guess + 50).filter { it > 0 }
    var text by remember(item.id) { mutableStateOf(item.unitGrams.roundedFormatted) }
    val kcalPerUnit = (item.food?.per100g(Nutrient.KCAL) ?: 0.0) * item.unitGrams / 100

    Column(Modifier.padding(top = 8.dp, end = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Neue Einheit: „${item.unit}“ gibt es für ${item.food?.name.orEmpty()} noch nicht. Wie viel Gramm sind 1 ${item.unit}?",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { value ->
                    text = value.filter { it.isDigit() }
                    onChangeGrams(text.toDoubleOrNull() ?: 0.0)
                },
                suffix = { Text("g") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.widthIn(max = 120.dp),
            )
            presets.forEach { preset ->
                FilterChip(
                    selected = item.unitGrams == preset,
                    onClick = { text = preset.roundedFormatted; onChangeGrams(preset) },
                    label = { Text("${preset.roundedFormatted} g") },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "1 ${item.unit} = ${item.unitGrams.roundedFormatted} g · ${kcalPerUnit.roundedFormatted} kcal",
                style = MaterialTheme.typography.bodySmall,
                color = Labs.colors.inkMuted,
                modifier = Modifier.weight(1f),
            )
            Box {
                Button(onClick = onSave, enabled = !isSaving && item.unitGrams > 0) {
                    if (isSaving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Speichern")
                }
            }
        }
    }
}
