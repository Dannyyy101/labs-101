import Foundation
import Observation

/// One food recognized in the text, before it is tracked.
nonisolated struct PreviewItem: Identifiable, Hashable, Sendable {
    enum Status: Sendable {
        case ok
        /// the unit doesn't exist for the food yet, the user has to confirm its grams
        case newUnit
        case notFound
    }

    // ml are counted 1:1 as grams
    static let gramUnits: Set<String> = ["g", "ml"]

    static let unitGuesses: [String: Double] = [
        "tasse": 200, "glas": 200, "becher": 250, "schüssel": 300, "schale": 250, "portion": 150, "dose": 400,
        "flasche": 500, "scheibe": 30, "handvoll": 30, "esslöffel": 15, "teelöffel": 5, "stück": 100,
        "riegel": 40, "kugel": 60, "prise": 1,
    ]

    let id: Int
    let query: String
    var amount: Double
    var unit: String
    var food: FoodDetails?
    /// set while the food only exists in the open food database
    var openFoodID: Int?
    var portion: FoodPortion?
    /// grams per unit, guessed while the user still has to confirm a new unit
    var unitGrams: Double
    var status: Status

    init(extracted: ExtractedFood, id: Int) {
        let food = extracted.food
        let unit = extracted.unit ?? food?.portions.first(where: \.isDefault)?.label ?? "Stück"

        self.id = id
        self.query = extracted.query
        self.amount = extracted.amount
        self.unit = unit
        self.food = food
        self.openFoodID = extracted.openFoodId
        self.portion = nil
        self.unitGrams = Self.guessedGrams(for: unit)

        if food == nil {
            status = .notFound
        } else if Self.gramUnits.contains(unit) {
            unitGrams = 1
            status = .ok
        } else if let portion = food?.portions.first(where: { Self.sameLabel($0.label, unit) }) {
            self.portion = portion
            unitGrams = portion.grams
            status = .ok
        } else {
            status = .newUnit
        }
    }

    static func guessedGrams(for unit: String) -> Double {
        unitGuesses[unit.lowercased()] ?? 100
    }

    static func sameLabel(_ a: String, _ b: String) -> Bool {
        a.trimmingCharacters(in: .whitespaces).lowercased() == b.trimmingCharacters(in: .whitespaces).lowercased()
    }

    var isGramUnit: Bool { Self.gramUnits.contains(unit) }

    var grams: Double { amount * unitGrams }

    var kcal: Double {
        ((food?.per100g(.kcal) ?? 0) * grams / 100).rounded()
    }

    /// What gets sent to the backend: portions are tracked by count, everything else in grams.
    var trackedAmount: Double {
        portion == nil ? grams : amount
    }
}

@Observable
final class TextFoodModel {
    var text = ""
    var items: [PreviewItem] = []
    private(set) var isExtracting = false
    private(set) var isSubmitting = false
    private(set) var savingUnitIDs: Set<Int> = []
    var error: String?

    var pendingUnits: Int { items.count { $0.status == .newUnit } }
    var notFound: Int { items.count { $0.status == .notFound } }
    var totalKcal: Double { items.reduce(0) { $0 + $1.kcal } }

    var canExtract: Bool {
        !isExtracting && !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var canSubmit: Bool {
        !items.isEmpty && pendingUnits == 0 && notFound == 0 && !isSubmitting
    }

    func extract(using service: FoodService) async {
        guard canExtract else { return }
        error = nil
        isExtracting = true
        defer { isExtracting = false }
        do {
            let extracted = try await service.extractFoods(from: text)
            items = extracted.enumerated().map { PreviewItem(extracted: $1, id: $0) }
        } catch {
            self.error = String(localized: "Text konnte nicht erkannt werden")
        }
    }

    func remove(_ item: PreviewItem) {
        items.removeAll { $0.id == item.id }
    }

    /// Stores the confirmed unit as new portion of the food.
    func saveUnit(of item: PreviewItem, using service: FoodService) async {
        guard let food = item.food else { return }
        error = nil
        savingUnitIDs.insert(item.id)
        defer { savingUnitIDs.remove(item.id) }

        do {
            // a portion needs a food, so open foods get copied into our foods first
            let imported = if let openFoodID = item.openFoodID { try await service.importOpenFood(openFoodID) } else { food }
            guard let foodID = imported.id else { throw APIError.invalidResponse }

            let portions = try await service.addPortion(NewFoodPortion(label: item.unit, grams: item.unitGrams, isDefault: false), toFood: foodID)
            let portion = portions.first { PreviewItem.sameLabel($0.label, item.unit) }

            update(item.id) {
                $0.food = imported
                $0.food?.portions = portions
                $0.openFoodID = nil
                $0.portion = portion
                $0.status = portion == nil ? .newUnit : .ok
            }
        } catch {
            self.error = String(localized: "Einheit „\(item.unit)“ konnte nicht gespeichert werden")
        }
    }

    /// Tracks all items, returns whether all of them were tracked.
    func trackAll(meal: Meal, using service: FoodService) async -> Bool {
        guard canSubmit else { return false }
        isSubmitting = true
        error = nil
        defer { isSubmitting = false }

        do {
            for item in items {
                if let openFoodID = item.openFoodID {
                    try await service.trackOpenFood(openFoodID, amount: item.trackedAmount, portionID: item.portion?.id, meal: meal)
                } else if let foodID = item.food?.id {
                    try await service.track(foodID: foodID, amount: item.trackedAmount, portionID: item.portion?.id, meal: meal)
                }
                // tracked items leave the list, so a retry after an error doesn't track them twice
                remove(item)
            }
            text = ""
            return true
        } catch {
            self.error = String(localized: "Eintragen fehlgeschlagen")
            return false
        }
    }

    func update(_ id: Int, _ change: (inout PreviewItem) -> Void) {
        guard let index = items.firstIndex(where: { $0.id == id }) else { return }
        change(&items[index])
    }
}
