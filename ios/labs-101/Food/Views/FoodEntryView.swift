import SwiftUI

/// Tracks a food or changes / deletes a tracked one.
struct FoodEntryView: View {
    enum Mode: Hashable {
        case create(foodID: Int)
        /// an open food only gets imported into our foods when it is tracked
        case createOpenFood(openFoodID: Int)
        case edit(TrackedFood)
    }

    let mode: Mode
    let onDone: () -> Void

    @Environment(\.foodService) private var foodService

    @State private var food: FoodDetails?
    @State private var amount: Double = 100
    @State private var portionID: Int?
    @State private var meal: Meal
    @State private var loadError: String?
    @State private var actionError: String?
    @State private var isSaving = false
    @State private var showDeleteConfirmation = false
    @FocusState private var amountFocused: Bool

    init(mode: Mode, meal: Meal, onDone: @escaping () -> Void) {
        self.mode = mode
        self.onDone = onDone
        _meal = State(initialValue: meal)
    }

    private var isEditing: Bool {
        if case .edit = mode { true } else { false }
    }

    private var grams: Double {
        amount * (food?.portions.first { $0.id == portionID }?.grams ?? 1)
    }

    var body: some View {
        content
            .navigationTitle(food?.name ?? "")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(isEditing ? "Speichern" : "Hinzufügen", systemImage: "checkmark", role: .confirm) {
                        Task { await save() }
                    }
                    .disabled(food == nil || amount <= 0 || isSaving)
                }
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button("Fertig") { amountFocused = false }
                }
            }
            .alert("Fehler", isPresented: $actionError.isPresent) {} message: {
                Text(actionError ?? "")
            }
            .task(id: mode) { await load() }
    }

    @ViewBuilder
    private var content: some View {
        if let food {
            form(for: food)
        } else if let loadError {
            ContentUnavailableView {
                Label("Nicht geladen", systemImage: "exclamationmark.triangle")
            } description: {
                Text(loadError)
            } actions: {
                Button("Erneut versuchen") { Task { await load() } }
            }
        } else {
            ProgressView()
        }
    }

    private func form(for food: FoodDetails) -> some View {
        Form {
            Section {
                VStack(spacing: 16) {
                    VStack(spacing: 0) {
                        Text(food.amount(of: .kcal, grams: grams).roundedFormatted)
                            .font(.system(size: 44, weight: .semibold))
                            .monospacedDigit()
                            .contentTransition(.numericText())
                        Text("kcal")
                            .foregroundStyle(.secondary)
                    }
                    HStack(spacing: 32) {
                        macro("Protein", food.amount(of: .protein, grams: grams), .protein)
                        macro("Kohlenhydrate", food.amount(of: .carbohydrates, grams: grams), .carbohydrates)
                        macro("Fett", food.amount(of: .fat, grams: grams), .fat)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .animation(.snappy, value: grams)
            }

            Section("Menge") {
                if !food.portions.isEmpty {
                    Picker("Einheit", selection: $portionID) {
                        Text("Gramm").tag(Int?.none)
                        ForEach(food.portions) { portion in
                            Text("\(portion.label) (\(portion.grams.compactFormatted) g)").tag(Int?.some(portion.id))
                        }
                    }
                }
                LabeledContent("Menge") {
                    HStack {
                        TextField("Menge", value: $amount, format: .number)
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                            .focused($amountFocused)
                        Text(food.portions.first { $0.id == portionID }?.label ?? "g")
                            .foregroundStyle(.secondary)
                    }
                }
            }

            Section {
                Picker("Mahlzeit", selection: $meal) {
                    ForEach(Meal.allCases) { meal in
                        Text(meal.label).tag(meal)
                    }
                }
            }

            if case .edit(let entry) = mode {
                Section {
                    Button("Eintrag löschen", role: .destructive) {
                        showDeleteConfirmation = true
                    }
                    .frame(maxWidth: .infinity)
                    .confirmationDialog("\(food.name) entfernen?", isPresented: $showDeleteConfirmation, titleVisibility: .visible) {
                        Button("Entfernen", role: .destructive) {
                            Task { await delete(entry) }
                        }
                    }
                }
            }
        }
        .disabled(isSaving)
    }

    private func macro(_ title: LocalizedStringKey, _ value: Double, _ tint: Color) -> some View {
        VStack(spacing: 2) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
                .textCase(.uppercase)
            Text("\(value.compactFormatted) g")
                .font(.headline)
                .foregroundStyle(tint)
                .monospacedDigit()
        }
        .accessibilityElement(children: .combine)
    }

    // MARK: Actions

    private func load() async {
        loadError = nil
        do {
            let details = switch mode {
            case .create(let foodID): try await foodService.details(foodID: foodID)
            case .createOpenFood(let openFoodID): try await foodService.openFood(openFoodID)
            case .edit(let entry): try await foodService.details(foodID: entry.food.id)
            }
            switch mode {
            case .create, .createOpenFood:
                amount = details.lastEntry?.amount ?? 100
                portionID = nil
            case .edit(let entry):
                amount = entry.amount
                portionID = details.portions.contains { $0.id == entry.portion?.id } ? entry.portion?.id : nil
            }
            food = details
        } catch is CancellationError {
        } catch {
            loadError = error.localizedDescription
        }
    }

    private func save() async {
        isSaving = true
        defer { isSaving = false }
        do {
            switch mode {
            case .create(let foodID):
                try await foodService.track(foodID: foodID, amount: amount, portionID: portionID, meal: meal)
            case .createOpenFood(let openFoodID):
                try await foodService.trackOpenFood(openFoodID, amount: amount, portionID: portionID, meal: meal)
            case .edit(let entry):
                try await foodService.update(entry, amount: amount, portionID: portionID, meal: meal)
            }
            onDone()
        } catch {
            actionError = error.localizedDescription
        }
    }

    private func delete(_ entry: TrackedFood) async {
        isSaving = true
        defer { isSaving = false }
        do {
            try await foodService.delete(entry)
            onDone()
        } catch {
            actionError = error.localizedDescription
        }
    }
}

extension Optional {
    /// Binding helper for alerts driven by an optional value.
    var isPresent: Bool {
        get { self != nil }
        set { if !newValue { self = nil } }
    }
}
