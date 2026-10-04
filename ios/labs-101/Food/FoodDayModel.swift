import Foundation
import Observation

/// The food tracked on one day.
@Observable
final class FoodDayModel {
    private(set) var day: Date
    private(set) var entries: [TrackedFood] = []
    private(set) var isLoading = false
    /// set when loading the day failed, changes are reported through `actionError`
    private(set) var loadError: String?
    var actionError: String?

    let goals: NutritionGoals
    private let calendar: Calendar

    init(day: Date = .now, goals: NutritionGoals = .default, calendar: Calendar = .current) {
        self.day = calendar.startOfDay(for: day)
        self.goals = goals
        self.calendar = calendar
    }

    var isToday: Bool {
        calendar.isDateInToday(day)
    }

    func showDay(byAdding days: Int) {
        day = calendar.date(byAdding: .day, value: days, to: day) ?? day
    }

    func showToday() {
        day = calendar.startOfDay(for: .now)
    }

    func load(using service: FoodService) async {
        let requestedDay = day
        isLoading = true
        defer { isLoading = false }

        do {
            let entries = try await service.trackedFood(on: requestedDay, calendar: calendar)
            // the user may have switched the day in the meantime
            guard requestedDay == day else { return }
            self.entries = entries
            loadError = nil
        } catch is CancellationError {
            // a newer load replaced this one
        } catch {
            guard requestedDay == day else { return }
            loadError = error.localizedDescription
        }
    }

    func delete(_ entry: TrackedFood, using service: FoodService) async {
        // optimistic, so the row disappears right away
        let previous = entries
        entries.removeAll { $0.id == entry.id }
        do {
            try await service.delete(entry)
        } catch {
            entries = previous
            actionError = error.localizedDescription
        }
    }

    // MARK: Totals

    func entries(for meal: Meal) -> [TrackedFood] {
        entries.filter { $0.meal.type == meal }
    }

    func total(_ nutrient: Nutrient, of entries: [TrackedFood]? = nil) -> Double {
        (entries ?? self.entries)
            .filter { $0.amount > 0 }
            .reduce(0) { $0 + $1.amount(of: nutrient) }
    }

    var remainingKcal: Double {
        goals.kcal - total(.kcal)
    }
}
