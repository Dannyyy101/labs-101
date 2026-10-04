import Foundation
import HealthKit

/// How far one type has been synced, persisted in UserDefaults so a sync can
/// continue after the app was suspended or killed in the middle of it.
nonisolated struct HealthTypeSyncState: Codable, Sendable {
    /// archived HKQueryAnchor of the last uploaded batch, nil = sync from the beginning
    var anchor: Data?
    /// set while the type is synced from the beginning
    var fullSyncId: UUID?
    /// samples uploaded by the running full sync
    var fullSyncUploaded = 0

    var queryAnchor: HKQueryAnchor? {
        anchor.flatMap { try? NSKeyedUnarchiver.unarchivedObject(ofClass: HKQueryAnchor.self, from: $0) }
    }

    mutating func setAnchor(_ anchor: HKQueryAnchor) throws {
        self.anchor = try NSKeyedArchiver.archivedData(withRootObject: anchor, requiringSecureCoding: true)
    }
}

/// UserDefaults is thread safe, so this can be used from any isolation.
nonisolated struct HealthSyncStateStore: Sendable {
    private static let prefix = "healthSync.type."
    private static let lastSyncKey = "healthSync.lastSync"
    private static let lastCharacteristicsKey = "healthSync.lastCharacteristics"

    private var defaults: UserDefaults { .standard }

    func state(for type: HKSampleType) -> HealthTypeSyncState {
        defaults.data(forKey: Self.prefix + type.identifier)
            .flatMap { try? JSONDecoder().decode(HealthTypeSyncState.self, from: $0) } ?? HealthTypeSyncState()
    }

    func save(_ state: HealthTypeSyncState, for type: HKSampleType) {
        defaults.set(try? JSONEncoder().encode(state), forKey: Self.prefix + type.identifier)
    }

    /// Forgets all anchors, the next sync uploads everything again.
    func reset() {
        for key in defaults.dictionaryRepresentation().keys where key.hasPrefix(Self.prefix) {
            defaults.removeObject(forKey: key)
        }
        defaults.removeObject(forKey: Self.lastCharacteristicsKey)
    }

    /// Types with a running or not yet started full sync.
    func pendingFullSyncs(of types: [HKSampleType]) -> Int {
        types.count { state(for: $0).anchor == nil || state(for: $0).fullSyncId != nil }
    }

    var lastSync: Date? {
        get { defaults.object(forKey: Self.lastSyncKey) as? Date }
        nonmutating set { defaults.set(newValue, forKey: Self.lastSyncKey) }
    }

    /// The characteristics barely change, so they are uploaded only when they differ.
    var lastCharacteristics: Data? {
        get { defaults.data(forKey: Self.lastCharacteristicsKey) }
        nonmutating set { defaults.set(newValue, forKey: Self.lastCharacteristicsKey) }
    }
}
