import Foundation
import Observation

/// What the sync engine is doing, for the UI.
@Observable
final class HealthSyncStatus {
    static let shared = HealthSyncStatus()

    private(set) var isRunning = false
    private(set) var currentType: String?
    private(set) var isFullSync = false
    private(set) var uploadedInRun = 0
    private(set) var lastReport: HealthSyncReport?
    private(set) var lastSync: Date?
    /// types that haven't been synced completely yet
    private(set) var pendingFullSyncs = 0

    private init() {
        refresh()
    }

    func refresh() {
        let state = HealthSyncStateStore()
        lastSync = state.lastSync
        pendingFullSyncs = state.pendingFullSyncs(of: HealthTypeCatalog.syncedTypes)
    }

    func started() {
        isRunning = true
        uploadedInRun = 0
    }

    func syncing(_ identifier: String, isFullSync: Bool) {
        currentType = Self.displayName(of: identifier)
        self.isFullSync = isFullSync
    }

    func progress(uploaded: Int) {
        uploadedInRun = uploaded
    }

    func finished(_ report: HealthSyncReport) {
        isRunning = false
        currentType = nil
        lastReport = report
        refresh()
    }

    /// HKQuantityTypeIdentifierHeartRate → HeartRate
    static func displayName(of identifier: String) -> String {
        let prefixes = ["HKQuantityTypeIdentifier", "HKCategoryTypeIdentifier", "HKCorrelationTypeIdentifier",
                        "HKScoredAssessmentTypeIdentifier", "HKDataTypeIdentifier", "HK"]
        guard let prefix = prefixes.first(where: identifier.hasPrefix) else { return identifier }
        return String(identifier.dropFirst(prefix.count))
    }
}
