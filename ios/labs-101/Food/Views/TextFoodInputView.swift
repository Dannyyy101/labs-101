import SwiftUI

/// Tracks several foods at once from a sentence like "2 Eier und eine Banane".
struct TextFoodInputView: View {
    @Binding var meal: Meal
    let onTracked: () -> Void

    @Environment(\.foodService) private var foodService
    @State private var model = TextFoodModel()
    @FocusState private var textFocused: Bool

    var body: some View {
        List {
            Section {
                MealPicker(meal: $meal)
            }

            Section {
                TextField("z. B. 2 Eier, eine Tasse Cappuccino, 1 Banane und 30 g Mandeln", text: $model.text, axis: .vertical)
                    .lineLimit(3...8)
                    .focused($textFocused)
                    .submitLabel(.done)

                Button {
                    textFocused = false
                    Task { await model.extract(using: foodService) }
                } label: {
                    HStack {
                        Label("Erkennen", systemImage: "text.viewfinder")
                        if model.isExtracting {
                            Spacer()
                            ProgressView()
                        }
                    }
                }
                .disabled(!model.canExtract)
            } footer: {
                Text("Menge · Einheit · Lebensmittel, getrennt durch Komma oder „und“")
            }

            if !model.items.isEmpty {
                Section("Vorschau") {
                    ForEach(model.items) { item in
                        PreviewRow(
                            item: item,
                            isSavingUnit: model.savingUnitIDs.contains(item.id),
                            onChangeUnitGrams: { grams in model.update(item.id) { $0.unitGrams = grams } },
                            onSaveUnit: { Task { await model.saveUnit(of: item, using: foodService) } }
                        )
                        .swipeActions {
                            Button("Entfernen", systemImage: "trash", role: .destructive) {
                                model.remove(item)
                            }
                        }
                    }
                }
            }
        }
        .safeAreaInset(edge: .bottom) {
            if !model.items.isEmpty {
                footer
            }
        }
        .alert("Fehler", isPresented: $model.error.isPresent) {} message: {
            Text(model.error ?? "")
        }
        .animation(.default, value: model.items)
    }

    private var footerHint: String {
        if model.notFound > 0 {
            return String(localized: "\(model.notFound) nicht gefunden – bitte entfernen")
        }
        if model.pendingUnits > 0 {
            return String(localized: "\(model.pendingUnits) neue Einheit(en) bestätigen")
        }
        return String(localized: "\(model.items.count) Lebensmittel")
    }

    private var footer: some View {
        HStack {
            VStack(alignment: .leading) {
                Text(footerHint)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text("\(model.totalKcal.roundedFormatted) kcal")
                    .font(.title3.bold())
                    .monospacedDigit()
            }
            Spacer()
            Button {
                Task {
                    if await model.trackAll(meal: meal, using: foodService) {
                        onTracked()
                    }
                }
            } label: {
                if model.isSubmitting {
                    ProgressView()
                } else {
                    Text("Eintragen")
                }
            }
            .buttonStyle(.glassProminent)
            .controlSize(.large)
            .disabled(!model.canSubmit)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .glassEffect(in: .rect(cornerRadius: 24))
        .padding(.horizontal)
    }
}

private struct PreviewRow: View {
    let item: PreviewItem
    let isSavingUnit: Bool
    let onChangeUnitGrams: (Double) -> Void
    let onSaveUnit: () -> Void

    private var approx: String { item.status == .newUnit ? "≈ " : "" }

    private var subtitle: String {
        var parts: [String] = []
        switch item.status {
        case .notFound:
            parts.append(String(localized: "Nicht gefunden"))
        case _ where item.isGramUnit:
            parts.append("\(item.amount.compactFormatted) \(item.unit)")
        default:
            parts.append("\(item.amount.compactFormatted) \(item.unit) · \(approx)\(item.grams.roundedFormatted) g")
        }
        if item.openFoodID != nil {
            parts.append("Open Food Facts")
        }
        return parts.joined(separator: " · ")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                statusIcon
                VStack(alignment: .leading, spacing: 2) {
                    Text(item.food?.name ?? item.query)
                        .lineLimit(1)
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                if item.food != nil {
                    VStack(alignment: .trailing, spacing: 0) {
                        Text("\(approx)\(item.kcal.roundedFormatted)")
                            .fontWeight(.semibold)
                            .monospacedDigit()
                        Text("kcal")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            if item.status == .newUnit {
                NewUnitEditor(item: item, isSaving: isSavingUnit, onChangeGrams: onChangeUnitGrams, onSave: onSaveUnit)
            }
        }
        .padding(.vertical, 4)
    }

    @ViewBuilder
    private var statusIcon: some View {
        switch item.status {
        case .ok:
            Image(systemName: "checkmark.circle.fill").foregroundStyle(.green)
                .accessibilityLabel("Erkannt")
        case .newUnit:
            Image(systemName: "questionmark.circle.fill").foregroundStyle(.orange)
                .accessibilityLabel("Neue Einheit")
        case .notFound:
            Image(systemName: "xmark.circle.fill").foregroundStyle(.red)
                .accessibilityLabel("Nicht gefunden")
        }
    }
}

/// Asks how many grams one unit of the food has, e.g. 1 Tasse = 200 g.
private struct NewUnitEditor: View {
    let item: PreviewItem
    let isSaving: Bool
    let onChangeGrams: (Double) -> Void
    let onSave: () -> Void

    private var step: Double { item.unitGrams > 50 ? 10 : 5 }

    private var presets: [Double] {
        let guess = PreviewItem.guessedGrams(for: item.unit)
        return [guess - 50, guess, guess + 50].filter { $0 > 0 }
    }

    private var grams: Binding<Double> {
        Binding(get: { item.unitGrams }, set: { onChangeGrams(max(0, $0)) })
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("**Neue Einheit:** „\(item.unit)“ gibt es für \(item.food?.name ?? "") noch nicht. Wie viel Gramm sind 1 \(item.unit)?")
                .font(.subheadline)

            Stepper(value: grams, in: step...5000, step: step) {
                HStack(spacing: 4) {
                    TextField("Gramm", value: grams, format: .number)
                        .keyboardType(.numberPad)
                        .frame(maxWidth: 70)
                        .textFieldStyle(.roundedBorder)
                        .multilineTextAlignment(.trailing)
                    Text("g").foregroundStyle(.secondary)
                }
            }

            HStack {
                ForEach(presets, id: \.self) { preset in
                    Button("\(preset.roundedFormatted) g") { onChangeGrams(preset) }
                        .buttonStyle(.bordered)
                        .buttonBorderShape(.capsule)
                        .tint(item.unitGrams == preset ? .accentColor : .secondary)
                }
            }

            HStack {
                Text("1 \(item.unit) = \(item.unitGrams.roundedFormatted) g · \(((item.food?.per100g(.kcal) ?? 0) * item.unitGrams / 100).roundedFormatted) kcal")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer()
                Button {
                    onSave()
                } label: {
                    if isSaving {
                        ProgressView()
                    } else {
                        Text("Einheit speichern")
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(.orange)
                .disabled(isSaving || item.unitGrams <= 0)
            }
        }
        .padding(12)
        .background(.background.secondary, in: .rect(cornerRadius: 12))
        // the buttons inside must not trigger each other through the row
        .buttonStyle(.borderless)
    }
}
