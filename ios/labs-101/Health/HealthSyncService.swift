import Foundation

/// The health endpoints of the backend, for the signed in user.
nonisolated struct HealthSyncService: Sendable {
    private let client: APIClient
    private let basePath: String

    init(config: AppConfig = .current, session: URLSession = .shared) {
        self.client = APIClient(config: config, session: session)
        self.basePath = "users/me/health"
    }

    // MARK: Sync

    func upload(_ samples: [HealthSampleUpload], fullSyncId: UUID?) async throws {
        let _: HealthSyncResult = try await client.send("POST", "\(basePath)/samples",
            body: UploadHealthSamplesRequest(fullSyncId: fullSyncId, samples: samples))
    }

    func delete(_ uuids: [UUID]) async throws {
        let _: HealthSyncResult = try await client.send("POST", "\(basePath)/samples/delete",
            body: DeleteHealthSamplesRequest(uuids: uuids))
    }

    /// Removes the samples of the type the full sync didn't upload.
    func completeFullSync(type: String, fullSyncId: UUID) async throws {
        let _: HealthSyncResult = try await client.send("POST", "\(basePath)/sync/complete",
            body: CompleteFullSyncRequest(type: type, fullSyncId: fullSyncId))
    }

    func saveCharacteristics(_ characteristics: JSONValue) async throws {
        try await client.send("PUT", "\(basePath)/characteristics", body: characteristics)
    }

    func summary() async throws -> [HealthTypeSummary] {
        try await client.get("\(basePath)/types")
    }

    // MARK: Writing into HealthKit

    func pendingWriteRequests() async throws -> [HealthWriteRequest] {
        try await client.get("\(basePath)/write-requests", query: [URLQueryItem(name: "status", value: "PENDING")])
    }

    func acknowledgeWriteRequest(_ id: Int, sampleUUID: UUID?, error: String?) async throws {
        try await client.send("POST", "\(basePath)/write-requests/\(id)/ack",
            body: AckHealthWriteRequest(sampleUuid: sampleUUID, error: error))
    }
}
