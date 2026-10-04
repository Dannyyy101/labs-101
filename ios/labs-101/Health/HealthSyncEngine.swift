import HealthKit
import OSLog

nonisolated enum HealthStore {
    /// HealthKit recommends a single store per app.
    static let shared = HKHealthStore()
}

nonisolated struct HealthSyncReport: Sendable {
    var uploaded = 0
    var deleted = 0
    var written = 0
    /// the deadline passed or the run was cancelled, the next run continues where this one stopped
    var stoppedEarly = false
    /// another run was in progress, this call only asked it to run again
    var joined = false
    var error: String?
}

/// Pushes the HealthKit data to the backend, without message broker, just HTTP.
///
/// Every type is synced with an anchored query: HealthKit returns the samples
/// added and deleted since the anchor, which is stored after each uploaded
/// batch. A run can therefore stop at any point (the app gets only seconds in
/// the background) and the next one continues from there.
///
/// Without an anchor (first start, reinstalled app, "sync everything again")
/// a type is synced from the beginning. The backend upserts by HealthKit UUID,
/// so that never creates duplicates, and completing the full sync removes the
/// samples that were deleted in HealthKit in the meantime.
actor HealthSyncEngine {
    static let shared = HealthSyncEngine()

    private static let logger = Logger(subsystem: "labs-101", category: "HealthSync")

    private let store: HKHealthStore
    private let service: HealthSyncService
    private let encoder: HealthSampleEncoder
    private let state = HealthSyncStateStore()

    private var running: Task<HealthSyncReport, Never>?
    private var rerunRequested = false
    /// of the running sync, nil = no time limit
    private var deadline: Date?

    init(store: HKHealthStore = HealthStore.shared, service: HealthSyncService = HealthSyncService()) {
        self.store = store
        self.service = service
        self.encoder = HealthSampleEncoder(store: store)
    }

    /// Syncs all types. Calls while a run is in progress make it run once more.
    ///
    /// - Parameter deadline: background callers only have some seconds, the run
    ///   stops between two batches once it passed.
    @discardableResult
    func sync(deadline: Date? = nil) async -> HealthSyncReport {
        if let running {
            rerunRequested = true
            // e.g. the app was opened during a background sync, which may now take as long as it needs
            if let current = self.deadline {
                self.deadline = deadline.map { max($0, current) }
            }
            // a background caller can't wait for a long running foreground sync
            return deadline == nil ? await running.value : HealthSyncReport(joined: true)
        }

        self.deadline = deadline
        let task = Task { await runUntilIdle() }
        running = task
        let report = await task.value
        if running == task {
            running = nil
        }
        return report
    }

    /// Stops the running sync after the current batch.
    func cancel() {
        running?.cancel()
    }

    /// Forgets all anchors and uploads everything again.
    @discardableResult
    func resetAndSync() async -> HealthSyncReport {
        if let running {
            running.cancel()
            _ = await running.value
        }
        state.reset()
        return await sync()
    }

    private func runUntilIdle() async -> HealthSyncReport {
        var report = HealthSyncReport()
        await HealthSyncStatus.shared.started()
        repeat {
            rerunRequested = false
            report.stoppedEarly = false
            await runOnce(report: &report)
        } while rerunRequested && report.error == nil && !shouldStop()

        if report.error == nil && !report.stoppedEarly {
            state.lastSync = .now
        }
        Self.logger.info("Health sync finished: \(report.uploaded) uploaded, \(report.deleted) deleted, \(report.written) written, stopped early: \(report.stoppedEarly), error: \(report.error ?? "-")")
        await HealthSyncStatus.shared.finished(report)
        return report
    }

    private func runOnce(report: inout HealthSyncReport) async {
        do {
            // first, so the written samples come back in the same run
            report.written += try await HealthWriter(store: store, service: service).writePending()
            try await syncCharacteristics()

            for type in HealthTypeCatalog.syncedTypes {
                guard !shouldStop() else {
                    report.stoppedEarly = true
                    return
                }
                do {
                    try await sync(type, report: &report)
                } catch let error as HKError where error.code == .errorDatabaseInaccessible {
                    // the device is locked, HealthKit data is encrypted until it is unlocked
                    throw error
                } catch let error as HKError {
                    // e.g. a type the user was never asked for, the other types can still be synced
                    Self.logger.warning("Skipping \(type.identifier): \(error.localizedDescription)")
                }
            }
        } catch {
            if Task.isCancelled {
                report.stoppedEarly = true
            } else {
                report.error = error.localizedDescription
                Self.logger.error("Health sync failed: \(error.localizedDescription)")
            }
        }
    }

    private func sync(_ type: HKSampleType, report: inout HealthSyncReport) async throws {
        var typeState = state.state(for: type)
        if typeState.anchor == nil && typeState.fullSyncId == nil {
            typeState.fullSyncId = UUID()
            typeState.fullSyncUploaded = 0
            state.save(typeState, for: type)
        }
        await HealthSyncStatus.shared.syncing(type.identifier, isFullSync: typeState.fullSyncId != nil)

        while true {
            guard !shouldStop() else {
                report.stoppedEarly = true
                return
            }

            let query = HKAnchoredObjectQueryDescriptor(
                predicates: [.sample(type: type)],
                anchor: typeState.queryAnchor,
                limit: Self.batchSize(for: type))
            let result = try await query.result(for: store)
            if result.addedSamples.isEmpty && result.deletedObjects.isEmpty {
                try typeState.setAnchor(result.newAnchor)
                state.save(typeState, for: type)
                break
            }

            var uploads: [HealthSampleUpload] = []
            for sample in result.addedSamples {
                if let upload = await encoder.encode(sample) {
                    uploads.append(upload)
                }
            }
            if !uploads.isEmpty {
                try await service.upload(uploads, fullSyncId: typeState.fullSyncId)
            }
            let deleted = result.deletedObjects.map(\.uuid)
            if !deleted.isEmpty {
                try await service.delete(deleted)
            }

            // only after the backend has the batch, otherwise it is uploaded again next time
            try typeState.setAnchor(result.newAnchor)
            typeState.fullSyncUploaded += uploads.count
            state.save(typeState, for: type)
            report.uploaded += uploads.count
            report.deleted += deleted.count
            await HealthSyncStatus.shared.progress(uploaded: report.uploaded)
        }

        if let fullSyncId = typeState.fullSyncId {
            // HealthKit returns nothing instead of an error for types the user didn't allow to read,
            // so a full sync without any samples must not delete what the backend already has
            if typeState.fullSyncUploaded > 0 {
                try await service.completeFullSync(type: type.identifier, fullSyncId: fullSyncId)
            }
            typeState.fullSyncId = nil
            state.save(typeState, for: type)
        }
    }

    private func syncCharacteristics() async throws {
        var values: [String: JSONValue] = [:]
        if let birthday = try? store.dateOfBirthComponents(), let year = birthday.year, let month = birthday.month, let day = birthday.day {
            values["dateOfBirth"] = .string(String(format: "%04d-%02d-%02d", year, month, day))
        }
        if let sex = try? store.biologicalSex().biologicalSex, sex != .notSet {
            values["biologicalSex"] = .number(Double(sex.rawValue))
        }
        if let bloodType = try? store.bloodType().bloodType, bloodType != .notSet {
            values["bloodType"] = .number(Double(bloodType.rawValue))
        }
        if let skinType = try? store.fitzpatrickSkinType().skinType, skinType != .notSet {
            values["fitzpatrickSkinType"] = .number(Double(skinType.rawValue))
        }
        if let wheelchairUse = try? store.wheelchairUse().wheelchairUse, wheelchairUse != .notSet {
            values["wheelchairUse"] = .number(Double(wheelchairUse.rawValue))
        }
        if let moveMode = try? store.activityMoveMode().activityMoveMode {
            values["activityMoveMode"] = .number(Double(moveMode.rawValue))
        }
        guard !values.isEmpty else { return }

        let characteristics = JSONValue.object(values)
        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys
        let data = try encoder.encode(characteristics)
        guard data != state.lastCharacteristics else { return }
        try await service.saveCharacteristics(characteristics)
        state.lastCharacteristics = data
    }

    private func shouldStop() -> Bool {
        Task.isCancelled || deadline.map { Date.now >= $0 } ?? false
    }

    /// Small enough to stay well below 1 MB per request (default body limit of nginx).
    private static func batchSize(for type: HKSampleType) -> Int {
        switch type {
        case is HKQuantityType, is HKCategoryType, is HKCorrelationType: 500
        // with GPS route / voltage measurements, a long run alone can be close to 1 MB
        case is HKWorkoutType, is HKElectrocardiogramType: 1
        default: 100
        }
    }
}
