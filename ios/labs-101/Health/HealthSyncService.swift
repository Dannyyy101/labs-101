import Foundation
import LabsAPI

/// The health endpoints of the backend, for the signed in user.
nonisolated struct HealthSyncService: Sendable {
    private let client: APIClient

    init(config: AppConfig = .current, session: URLSession = .shared) {
        self.client = APIClient(config: config, session: session)
    }

    // MARK: Sync

    func upload(_ samples: [HealthSampleUpload], fullSyncId: UUID?) async throws {
        let body = Components.Schemas.UploadHealthSamplesDto(fullSyncId: fullSyncId?.uuidString, samples: try samples.map { try $0.dto })
        _ = try await client.call { try await $0.healthUploadSamples(body: .json(body)).ok }
    }

    func delete(_ uuids: [UUID]) async throws {
        let body = Components.Schemas.DeleteHealthSamplesDto(uuids: uuids.map(\.uuidString))
        _ = try await client.call { try await $0.healthDeleteSamples(body: .json(body)).ok }
    }

    /// Removes the samples of the type the full sync didn't upload.
    func completeFullSync(type: String, fullSyncId: UUID) async throws {
        let body = Components.Schemas.CompleteFullSyncDto(fullSyncId: fullSyncId.uuidString, _type: type)
        _ = try await client.call { try await $0.healthCompleteFullSync(body: .json(body)).ok }
    }

    func saveCharacteristics(_ characteristics: JSONValue) async throws {
        let body = try characteristics.objectContainer
        _ = try await client.call { try await $0.healthSaveCharacteristics(body: .json(body)).noContent }
    }

    func summary() async throws -> [HealthTypeSummary] {
        try await client.call { try await $0.healthGetSummary().ok.body.json }.map(HealthTypeSummary.init)
    }

    // MARK: Writing into HealthKit

    func pendingWriteRequests() async throws -> [HealthWriteRequest] {
        try await client.call { try await $0.healthGetWriteRequests(query: .init(status: .pending)).ok.body.json }
            .map(HealthWriteRequest.init)
    }

    func acknowledgeWriteRequest(_ id: Int, sampleUUID: UUID?, error: String?) async throws {
        let body = Components.Schemas.AckHealthWriteRequestDto(error: error, sampleUuid: sampleUUID?.uuidString)
        _ = try await client.call { try await $0.healthAcknowledgeWriteRequest(path: .init(id: Int64(id)), body: .json(body)).ok }
    }
}
