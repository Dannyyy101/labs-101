import Foundation

/// Kinds the backend distinguishes, see HealthSampleKind.java
nonisolated enum HealthSampleKind: String, Codable, Sendable {
    case quantity = "QUANTITY"
    case category = "CATEGORY"
    case workout = "WORKOUT"
    case correlation = "CORRELATION"
    case ecg = "ECG"
    case audiogram = "AUDIOGRAM"
    case stateOfMind = "STATE_OF_MIND"
    case scoredAssessment = "SCORED_ASSESSMENT"
}

/// One HealthKit sample as the backend stores it, `uuid` is the HealthKit UUID.
nonisolated struct HealthSampleUpload: Codable, Sendable {
    let uuid: UUID
    let kind: HealthSampleKind
    /// HealthKit identifier, e.g. HKQuantityTypeIdentifierHeartRate
    let type: String
    let startDate: Date
    let endDate: Date
    let value: Double?
    let unit: String?
    let sourceName: String?
    let sourceBundleId: String?
    let payload: JSONValue?
}

nonisolated struct UploadHealthSamplesRequest: Encodable, Sendable {
    /// only set while a type is synced from the beginning
    let fullSyncId: UUID?
    let samples: [HealthSampleUpload]
}

nonisolated struct DeleteHealthSamplesRequest: Encodable, Sendable {
    let uuids: [UUID]
}

nonisolated struct CompleteFullSyncRequest: Encodable, Sendable {
    let type: String
    let fullSyncId: UUID
}

nonisolated struct HealthSyncResult: Decodable, Sendable {
    let count: Int
}

/// What the backend has stored of one type.
nonisolated struct HealthTypeSummary: Decodable, Sendable, Identifiable {
    let type: String
    let kind: HealthSampleKind
    let count: Int
    let firstDate: Date?
    let lastDate: Date?
    let lastSyncedAt: Date?

    var id: String { type }
}

/// A sample created outside of the app (e.g. on the website) the app writes into HealthKit.
nonisolated struct HealthWriteRequest: Decodable, Sendable, Identifiable {
    let id: Int
    let kind: HealthSampleKind
    let type: String
    let startDate: Date
    let endDate: Date
    let value: Double
    let unit: String?
    let metadata: JSONValue?
}

nonisolated struct AckHealthWriteRequest: Encodable, Sendable {
    let sampleUuid: UUID?
    let error: String?
}
