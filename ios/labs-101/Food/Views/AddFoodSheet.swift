import SwiftUI

/// Finds food by name, barcode or free text and tracks it.
struct AddFoodSheet: View {
    enum Mode: Hashable {
        case search, text
    }

    let onTracked: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var meal: Meal
    @State private var mode = Mode.search
    @State private var path: [Int] = []

    init(meal: Meal, onTracked: @escaping () -> Void) {
        self.onTracked = onTracked
        _meal = State(initialValue: meal)
    }

    var body: some View {
        NavigationStack(path: $path) {
            Group {
                switch mode {
                case .search:
                    FoodSearchView(meal: $meal) { foodID in path.append(foodID) }
                case .text:
                    TextFoodInputView(meal: $meal, onTracked: finish)
                }
            }
            .navigationTitle("Essen hinzufügen")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(role: .close) { dismiss() }
                }
                ToolbarItem(placement: .principal) {
                    Picker("Eingabe", selection: $mode) {
                        Text("Suchen").tag(Mode.search)
                        Text("Per Text").tag(Mode.text)
                    }
                    .pickerStyle(.segmented)
                    .fixedSize()
                }
            }
            .navigationDestination(for: Int.self) { foodID in
                FoodEntryView(mode: .create(foodID: foodID), meal: meal, onDone: finish)
            }
        }
    }

    private func finish() {
        onTracked()
        dismiss()
    }
}

struct MealPicker: View {
    @Binding var meal: Meal

    var body: some View {
        Picker("Mahlzeit", selection: $meal) {
            ForEach(Meal.allCases) { meal in
                Text(meal.label).tag(meal)
            }
        }
    }
}

/// Searches our foods by name, page by page, and looks up scanned barcodes.
private struct FoodSearchView: View {
    @Binding var meal: Meal
    let onSelect: (Int) -> Void

    @Environment(\.foodService) private var foodService
    @State private var query = ""
    @State private var results: [FoodSearchResult] = []
    @State private var nextPage: Int?
    @State private var isLoading = false
    @State private var error: String?
    @State private var showScanner = false

    private var trimmedQuery: String {
        query.trimmingCharacters(in: .whitespaces)
    }

    var body: some View {
        List {
            Section {
                MealPicker(meal: $meal)
            }

            Section {
                ForEach(results) { food in
                    Button(food.name) { onSelect(food.id) }
                        .tint(.primary)
                        .onAppear {
                            if food.id == results.last?.id {
                                Task { await loadNextPage() }
                            }
                        }
                }
                if isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                }
            }
        }
        .overlay {
            if trimmedQuery.isEmpty && results.isEmpty && !isLoading {
                ContentUnavailableView(
                    "Lebensmittel suchen",
                    systemImage: "magnifyingglass",
                    description: Text("Suche nach dem Namen oder scanne den Barcode einer Verpackung.")
                )
            } else if !trimmedQuery.isEmpty && results.isEmpty && !isLoading {
                ContentUnavailableView.search(text: trimmedQuery)
            }
        }
        .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: "Suchen…")
        .autocorrectionDisabled()
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Barcode scannen", systemImage: "barcode.viewfinder") {
                    showScanner = true
                }
            }
        }
        .sheet(isPresented: $showScanner) {
            BarcodeScannerView { code in
                showScanner = false
                Task { await lookUp(barcode: code) }
            }
        }
        .alert("Fehler", isPresented: $error.isPresent) {} message: {
            Text(error ?? "")
        }
        .task(id: trimmedQuery) {
            await search(trimmedQuery)
        }
    }

    private func search(_ name: String) async {
        guard !name.isEmpty else {
            results = []
            nextPage = nil
            return
        }
        // debounce typing, the task gets cancelled by the next keystroke
        try? await Task.sleep(for: .milliseconds(250))
        guard !Task.isCancelled else { return }

        isLoading = true
        defer { isLoading = false }
        do {
            let page = try await foodService.search(name)
            results = page.content
            nextPage = page.last ? nil : page.number + 1
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func loadNextPage() async {
        guard let page = nextPage, !isLoading else { return }
        let name = trimmedQuery
        isLoading = true
        defer { isLoading = false }
        do {
            let next = try await foodService.search(name, page: page)
            guard name == trimmedQuery else { return }
            results += next.content
            nextPage = next.last ? nil : next.number + 1
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func lookUp(barcode: String) async {
        isLoading = true
        defer { isLoading = false }
        do {
            let food = try await foodService.food(barcode: barcode)
            onSelect(food.id)
        } catch {
            self.error = error.localizedDescription
        }
    }
}
