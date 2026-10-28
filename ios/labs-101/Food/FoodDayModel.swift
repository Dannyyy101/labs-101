import Foundation
import HealthKit
import Observation

/// The food tracked on one day.
@Observable
final class FoodDayModel {
    private(set) var day: Date
    private(set) var entries: [TrackedFood] = []
    /// active energy of the day from HealthKit, it adds to the calorie goal
    private(set) var burnedKcal: Double = 0
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

        async let burned = activeEnergy(on: requestedDay)
        do {
            let entries = try await service.trackedFood(on: requestedDay, calendar: calendar)
            // the user may have switched the day in the meantime
            guard requestedDay == day else { return }
            self.entries = entries
            burnedKcal = await burned
            loadError = nil
            updateEveningReminder()
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
            updateEveningReminder()
        } catch {
            entries = previous
            actionError = error.localizedDescription
        }
    }

    /// summed like Apple Health does it, so iPhone and Watch don't count twice; 0 without access
    private func activeEnergy(on day: Date) async -> Double {
        let end = calendar.date(byAdding: .day, value: 1, to: day) ?? day
        let query = HKStatisticsQueryDescriptor(
            predicate: .quantitySample(type: HKQuantityType(.activeEnergyBurned),
                                       predicate: HKQuery.predicateForSamples(withStart: day, end: end)),
            options: .cumulativeSum)
        let sum = try? await query.result(for: HealthStore.shared)?.sumQuantity()
        return sum?.doubleValue(for: .kilocalorie()) ?? 0
    }

    /// the reminder at 20:00 shows the numbers of the last change
    private func updateEveningReminder() {
        guard isToday else { return }
        let (entries, goals, calendar) = (entries, goals, calendar)
        Task { await AppNotifications.updateEveningReminder(entries: entries, goals: goals, calendar: calendar) }
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
        goals.kcal + burnedKcal - total(.kcal)
    }
}
