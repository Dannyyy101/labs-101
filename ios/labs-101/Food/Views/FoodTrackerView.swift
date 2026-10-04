import SwiftUI

/// Day overview of the tracked food, the iOS counterpart of /foods/track.
struct FoodTrackerView: View {
    @Environment(\.foodService) private var foodService
    @State private var model = FoodDayModel()
    @State private var addingToMeal: Meal?
    @State private var editingEntry: TrackedFood?

    var body: some View {
        NavigationStack {
            content
                .navigationTitle(model.day.dayTitle())
                .navigationSubtitle(model.day.longDay)
                .toolbar { toolbar }
                .refreshable { await model.load(using: foodService) }
                .task(id: model.day) { await model.load(using: foodService) }
                .sheet(item: $addingToMeal) { meal in
                    AddFoodSheet(meal: meal, onTracked: reload)
                }
                .sheet(item: $editingEntry) { entry in
                    NavigationStack {
                        FoodEntryView(mode: .edit(entry), meal: entry.meal.type) {
                            editingEntry = nil
                            reload()
                        }
                        .toolbar {
                            ToolbarItem(placement: .cancellationAction) {
                                Button(role: .close) { editingEntry = nil }
                            }
                        }
                    }
                }
                .alert("Fehler", isPresented: $model.actionError.isPresent) {} message: {
                    Text(model.actionError ?? "")
                }
        }
    }

    @ViewBuilder
    private var content: some View {
        if let loadError = model.loadError, model.entries.isEmpty {
            ContentUnavailableView {
                Label("Keine Verbindung", systemImage: "wifi.exclamationmark")
            } description: {
                Text(loadError)
            } actions: {
                Button("Erneut versuchen") { reload() }
                    .buttonStyle(.borderedProminent)
            }
        } else {
            List {
                Section {
                    DaySummaryView(consumed: { model.total($0) }, goals: model.goals)
                }

                ForEach(Meal.allCases) { meal in
                    MealSection(
                        meal: meal,
                        entries: model.entries(for: meal),
                        remainingKcal: model.remainingKcal,
                        onAdd: { addingToMeal = meal },
                        onSelect: { editingEntry = $0 },
                        onDelete: { entry in Task { await model.delete(entry, using: foodService) } }
                    )
                }
            }
            .listSectionSpacing(.compact)
            .overlay {
                if model.isLoading && model.entries.isEmpty {
                    ProgressView()
                }
            }
        }
    }

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        ToolbarItemGroup(placement: .topBarLeading) {
            Button("Vorheriger Tag", systemImage: "chevron.left") {
                model.showDay(byAdding: -1)
            }
            Button("Nächster Tag", systemImage: "chevron.right") {
                model.showDay(byAdding: 1)
            }
        }
        if !model.isToday {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Heute") { model.showToday() }
            }
            ToolbarSpacer(.fixed, placement: .topBarTrailing)
        }
        ToolbarItem(placement: .topBarTrailing) {
            Button("Essen hinzufügen", systemImage: "plus") {
                addingToMeal = .current()
            }
            .buttonStyle(.glassProminent)
        }
    }

    private func reload() {
        Task { await model.load(using: foodService) }
    }
}

#Preview {
    FoodTrackerView()
}
