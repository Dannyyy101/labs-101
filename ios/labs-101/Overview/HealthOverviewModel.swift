import Foundation
import Observation

/// The health overview of one day, the data of the home page in the web.
@Observable
final class HealthOverviewModel {
    private(set) var day: Date
    private(set) var overview: HealthOverview?
    private(set) var isLoading = false
    /// set when loading the day failed
    private(set) var loadError: String?

    let calendar: Calendar

    init(day: Date = .now, calendar: Calendar = .current) {
        self.day = calendar.startOfDay(for: day)
        self.calendar = calendar
    }

    var isToday: Bool {
        calendar.isDateInToday(day)
    }

    func showDay(byAdding days: Int) {
        guard let next = calendar.date(byAdding: .day, value: days, to: day) else { return }
        show(next)
    }

    func show(_ date: Date) {
        // there is nothing to show in the future
        day = min(calendar.startOfDay(for: date), calendar.startOfDay(for: .now))
    }

    func showToday() {
        day = calendar.startOfDay(for: .now)
    }

    func load(using service: OverviewService) async {
        let requestedDay = day
        isLoading = true
        defer { isLoading = false }

        do {
            let overview = try await service.overview(on: requestedDay, calendar: calendar)
            // the user may have switched the day in the meantime
            guard requestedDay == day else { return }
            self.overview = overview
            loadError = nil
        } catch is CancellationError {
            // a newer load replaced this one
        } catch {
            guard requestedDay == day else { return }
            loadError = error.localizedDescription
        }
    }
}
