import Foundation
import SwiftUI

/// The health overview endpoint of the backend, for the signed in user.
nonisolated struct OverviewService: Sendable {
    private let client: APIClient

    init(config: AppConfig = .current, session: URLSession = .shared) {
        self.client = APIClient(config: config, session: session)
    }

    /// The overview of the day in the time zone of the calendar.
    func overview(on day: Date, calendar: Calendar = .current) async throws -> HealthOverview {
        try await client.get("users/me/health/overview", query: [
            URLQueryItem(name: "date", value: day.isoDay(calendar: calendar)),
            URLQueryItem(name: "zone", value: calendar.timeZone.identifier),
        ])
    }
}

extension EnvironmentValues {
    @Entry var overviewService = OverviewService()
}

nonisolated extension Date {
    /// "2026-10-04"
    func isoDay(calendar: Calendar = .current) -> String {
        let components = calendar.dateComponents([.year, .month, .day], from: self)
        return String(format: "%04d-%02d-%02d", components.year ?? 0, components.month ?? 0, components.day ?? 0)
    }

    /// Start of the day "2026-10-04" in the calendar.
    init?(isoDay: String, calendar: Calendar = .current) {
        let parts = isoDay.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3,
              let date = calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
        else { return nil }
        self = date
    }
}
