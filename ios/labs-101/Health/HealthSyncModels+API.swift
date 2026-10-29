import Foundation
import LabsAPI

// the models of the app as the DTOs of the backend (generated from api/openapi.json) and back

nonisolated extension HealthSampleUpload {
    var dto: Components.Schemas.HealthSampleDto {
        get throws {
            guard let kind = Components.Schemas.HealthSampleDto.KindPayload(rawValue: kind.rawValue) else {
                throw APIError.invalidResponse
            }
            return .init(endDate: endDate, kind: kind, payload: try payload.map { try $0.objectContainer }, sourceBundleId: sourceBundleId,
                         sourceName: sourceName, startDate: startDate, _type: type, unit: unit, uuid: uuid.uuidString,
                         value: value)
        }
    }
}

nonisolated extension HealthTypeSummary {
    init(_ dto: Components.Schemas.HealthTypeSummaryDto) throws {
        guard let kind = HealthSampleKind(rawValue: dto.kind.rawValue) else { throw APIError.invalidResponse }
        self.init(type: dto._type, kind: kind, count: Int(dto.count), firstDate: dto.firstDate, lastDate: dto.lastDate,
                  lastSyncedAt: dto.lastSyncedAt)
    }
}

nonisolated extension HealthWriteRequest {
    /// Only quantity and category samples with a value can be written.
    init(_ dto: Components.Schemas.HealthWriteRequestDto) throws {
        guard let kind = HealthSampleKind(rawValue: dto.kind.rawValue), let value = dto.value else {
            throw APIError.invalidResponse
        }
        self.init(id: Int(dto.id), kind: kind, type: dto._type, startDate: dto.startDate, endDate: dto.endDate, value: value,
                  unit: dto.unit, metadata: try dto.metadata.map(JSONValue.init))
    }
}

nonisolated extension JSONValue {
    /// Free JSON of HealthKit, the client of the backend has its own type for it.
    var objectContainer: OpenAPIObjectContainer {
        get throws {
            try JSONDecoder().decode(OpenAPIObjectContainer.self, from: JSONEncoder().encode(self))
        }
    }

    init(_ container: OpenAPIObjectContainer) throws {
        self = try JSONDecoder().decode(JSONValue.self, from: JSONEncoder().encode(container))
    }
}
