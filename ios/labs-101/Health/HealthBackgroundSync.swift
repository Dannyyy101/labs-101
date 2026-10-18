import BackgroundTasks
import HealthKit

/// Keeps the backend up to date while the app is closed.
///
/// - HealthKit background delivery wakes the app when new samples arrive and
///   calls the observer query, which syncs for some seconds: the types with
///   new samples first, so e.g. the sleep of the night is uploaded (and its
///   notification sent) even when there is a lot of other data to catch up on.
/// - BGAppRefreshTask / BGProcessingTask are the fallback for missed
///   wake-ups and continue long (full) syncs, a processing task may run for minutes.
///
/// Both only work as long as the app is installed and signed, with a free
/// developer account that's 7 days after the last build.
enum HealthBackgroundSync {
    nonisolated static let refreshTaskID = "labs-101.labs-101.health-refresh"
    nonisolated static let processingTaskID = "labs-101.labs-101.health-processing"

    /// Background delivery lets the app run for ~30 s only, the notifications
    /// after the sync (overview, inbox) need some seconds of that too.
    private nonisolated static let observerTimeLimit: TimeInterval = 15
    private nonisolated static let refreshTimeLimit: TimeInterval = 20

    /// Woken right away for these, they trigger notifications. All other types
    /// hourly: waking the app for every heart rate sample uses up the
    /// background budget iOS grants it, and each wake-up syncs all types anyway.
    private nonisolated static let immediateTypes: Set<String> = Set(HealthSyncEngine.importantTypes.map(\.identifier))
    /// Raise when the frequencies change, they are set again once then.
    private nonisolated static let deliveryVersion = 2
    private static let deliveryVersionKey = "healthSync.backgroundDeliveryVersion"

    private static var observerQuery: HKObserverQuery?

    /// Has to be called in `application(_:didFinishLaunchingWithOptions:)`,
    /// HealthKit delivers the pending updates to the observer queries running by then.
    static func registerAtLaunch() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: refreshTaskID, using: nil) { task in
            handle(task, timeLimit: refreshTimeLimit)
        }
        BGTaskScheduler.shared.register(forTaskWithIdentifier: processingTaskID, using: nil) { task in
            handle(task, timeLimit: nil)
        }
        if HealthAuthorization.wasRequested {
            startObserving()
        }
    }

    static func startObserving() {
        guard observerQuery == nil, HKHealthStore.isHealthDataAvailable() else { return }

        // correlations are made of quantity samples, they change together with those
        let types = HealthTypeCatalog.syncedTypes.filter { !($0 is HKCorrelationType) }
        let query = makeObserverQuery(for: types)
        HealthStore.shared.execute(query)
        observerQuery = query

        Task {
            // older builds asked for .immediate for every type
            if UserDefaults.standard.integer(forKey: deliveryVersionKey) != deliveryVersion {
                try? await HealthStore.shared.disableAllBackgroundDelivery()
                UserDefaults.standard.set(deliveryVersion, forKey: deliveryVersionKey)
            }
            for type in types {
                // HealthKit limits some types (e.g. steps) to hourly updates on its own
                let frequency: HKUpdateFrequency = immediateTypes.contains(type.identifier) ? .immediate : .hourly
                try? await HealthStore.shared.enableBackgroundDelivery(for: type, frequency: frequency)
            }
        }
    }

    /// Submitting replaces the pending requests, so this can be called any time.
    nonisolated static func scheduleBackgroundTasks() {
        let refresh = BGAppRefreshTaskRequest(identifier: refreshTaskID)
        refresh.earliestBeginDate = .now.addingTimeInterval(30 * 60)
        try? BGTaskScheduler.shared.submit(refresh)

        let processing = BGProcessingTaskRequest(identifier: processingTaskID)
        processing.requiresNetworkConnectivity = true
        processing.earliestBeginDate = .now.addingTimeInterval(2 * 60 * 60)
        try? BGTaskScheduler.shared.submit(processing)
    }

    // HealthKit and BGTaskScheduler call these on background queues

    private nonisolated static func makeObserverQuery(for types: [HKSampleType]) -> HKObserverQuery {
        let descriptors = types.map { HKQueryDescriptor(sampleType: $0, predicate: nil) }
        return HKObserverQuery(queryDescriptors: descriptors) { _, updatedTypes, completionHandler, error in
            guard error == nil else {
                completionHandler()
                return
            }
            Task {
                let started = Date.now
                let types = Array(updatedTypes ?? [])
                let report = await HealthSyncEngine.shared.sync(deadline: started.addingTimeInterval(observerTimeLimit), first: types)
                // a run on a locked device isn't retried: the next (at most hourly) wake-up after unlocking
                // syncs the samples, sleep and workouts first
                HealthSyncLog.add(.observer, types: types, started: started, report: report)
                // without calling it HealthKit stops waking the app up
                completionHandler()
            }
        }
    }

    private nonisolated static func handle(_ task: BGTask, timeLimit: TimeInterval?) {
        scheduleBackgroundTasks()
        let sync = Task {
            let started = Date.now
            let report = await HealthSyncEngine.shared.sync(deadline: timeLimit.map { started.addingTimeInterval($0) })
            HealthSyncLog.add(task is BGProcessingTask ? .processing : .refresh, started: started, report: report)
            task.setTaskCompleted(success: report.error == nil)
        }
        task.expirationHandler = {
            sync.cancel()
            Task { await HealthSyncEngine.shared.cancel() }
        }
    }
}

enum HealthAuthorization {
    private static let requestedKey = "healthSync.authorizationRequested"

    static var wasRequested: Bool {
        UserDefaults.standard.bool(forKey: requestedKey)
    }

    /// True until the user was asked for all types of the catalog, so new
    /// types of a later build are asked for too.
    static func needsRequest() async -> Bool {
        guard HKHealthStore.isHealthDataAvailable() else { return false }
        let status = try? await HealthStore.shared.statusForAuthorizationRequest(
            toShare: HealthTypeCatalog.shareTypes, read: HealthTypeCatalog.readTypes)
        if status == .unnecessary {
            markRequested()
        }
        return status != .unnecessary
    }

    /// HealthKit doesn't tell whether the user allowed reading a type, types
    /// without permission just look empty.
    static func request() async throws {
        try await HealthStore.shared.requestAuthorization(
            toShare: HealthTypeCatalog.shareTypes, read: HealthTypeCatalog.readTypes)
        markRequested()
    }

    private static func markRequested() {
        UserDefaults.standard.set(true, forKey: requestedKey)
        HealthBackgroundSync.startObserving()
    }
}
