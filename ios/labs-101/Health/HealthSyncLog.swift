import Foundation
import HealthKit

/// One run of the sync in the background, to see afterwards when iOS woke
/// the app and how far it got (e.g. "locked" all night).
nonisolated struct HealthSyncLogEntry: Codable, Sendable, Identifiable {
    enum Trigger: String, Codable, Sendable {
        /// HealthKit background delivery, new samples
        case observer
        case refresh
        case processing

        var label: String {
            switch self {
            case .observer: "Neue Health-Daten"
            case .refresh: "Hintergrundaktualisierung"
            case .processing: "Hintergrundverarbeitung"
            }
        }
    }

    var id: Date { date }
    var date: Date
    var trigger: Trigger
    /// the types HealthKit reported as changed, short names ("SleepAnalysis")
    var types: [String]
    var uploaded: Int
    var stoppedEarly: Bool
    var joined: Bool
    var locked: Bool
    var error: String?
    var seconds: Double
}

/// The last background runs, newest first, kept in UserDefaults.
nonisolated enum HealthSyncLog {
    private static let key = "healthSync.backgroundLog"
    private static let maxEntries = 50

    static var entries: [HealthSyncLogEntry] {
        UserDefaults.standard.data(forKey: key)
            .flatMap { try? JSONDecoder().decode([HealthSyncLogEntry].self, from: $0) } ?? []
    }

    static func add(_ trigger: HealthSyncLogEntry.Trigger, types: [HKSampleType] = [], started: Date, report: HealthSyncReport) {
        let entry = HealthSyncLogEntry(
            date: started, trigger: trigger, types: types.map(shortName), uploaded: report.uploaded,
            stoppedEarly: report.stoppedEarly, joined: report.joined, locked: report.locked, error: report.error,
            seconds: Date.now.timeIntervalSince(started))
        let all = [entry] + entries.prefix(maxEntries - 1)
        UserDefaults.standard.set(try? JSONEncoder().encode(all), forKey: key)
    }

    static func clear() {
        UserDefaults.standard.removeObject(forKey: key)
    }

    /// "HKCategoryTypeIdentifierSleepAnalysis" → "SleepAnalysis"
    private static func shortName(_ type: HKSampleType) -> String {
        for prefix in ["HKQuantityTypeIdentifier", "HKCategoryTypeIdentifier", "HKCorrelationTypeIdentifier", "HK"]
        where type.identifier.hasPrefix(prefix) {
            return String(type.identifier.dropFirst(prefix.count))
        }
        return type.identifier
    }
}
