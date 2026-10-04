import HealthKit

/// Writes samples created outside of the app (e.g. on the website) into HealthKit.
///
/// Every sample carries a sync identifier with the id of its write request, so
/// writing the same request twice (the app was killed before acknowledging it)
/// doesn't create a duplicate, and the backend can recognize the sample when
/// it comes back through the normal sync.
nonisolated struct HealthWriter: Sendable {
    let store: HKHealthStore
    let service: HealthSyncService

    static func syncIdentifier(forRequest id: Int) -> String {
        "labs-101.write-request.\(id)"
    }

    /// Returns the number of written samples. Requests HealthKit refused are
    /// acknowledged as failed, network errors are thrown.
    func writePending() async throws -> Int {
        var written = 0
        for request in try await service.pendingWriteRequests() {
            let sampleUUID: UUID?
            let failure: String?
            do {
                let sample = try Self.sample(for: request)
                try await store.save(sample)
                sampleUUID = sample.uuid
                failure = nil
                written += 1
            } catch {
                sampleUUID = nil
                failure = error.localizedDescription
            }
            try await service.acknowledgeWriteRequest(request.id, sampleUUID: sampleUUID, error: failure)
        }
        return written
    }

    // HealthKit raises Objective-C exceptions (which crash the app) for invalid
    // units, category values or date ranges, so everything is checked up front.
    static func sample(for request: HealthWriteRequest) throws -> HKSample {
        guard request.endDate >= request.startDate, request.value.isFinite, request.value >= 0 else {
            throw HealthWriteError.invalidValue
        }

        var metadata = request.metadata.flatMap { metadata -> [String: Any]? in
            guard case .object(let values) = metadata else { return nil }
            return values.compactMapValues(\.metadataValue)
        } ?? [:]
        metadata[HKMetadataKeySyncIdentifier] = syncIdentifier(forRequest: request.id)
        metadata[HKMetadataKeySyncVersion] = 1

        switch request.kind {
        case .quantity:
            guard let type = HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier(rawValue: request.type)),
                  HealthTypeCatalog.shareTypes.contains(type) else {
                throw HealthWriteError.unsupportedType(request.type)
            }
            guard let unitString = request.unit, supportedUnits.contains(unitString),
                  type.is(compatibleWith: HKUnit(from: unitString)) else {
                throw HealthWriteError.unsupportedUnit(request.unit ?? "")
            }
            let quantity = HKQuantity(unit: HKUnit(from: unitString), doubleValue: request.value)
            return HKQuantitySample(type: type, quantity: quantity, start: request.startDate, end: request.endDate, metadata: metadata)

        case .category:
            guard let type = HKObjectType.categoryType(forIdentifier: HKCategoryTypeIdentifier(rawValue: request.type)),
                  let values = categoryValues[type.identifier] else {
                throw HealthWriteError.unsupportedType(request.type)
            }
            guard request.value.rounded() == request.value, values.contains(Int(request.value)) else {
                throw HealthWriteError.invalidValue
            }
            return HKCategorySample(type: type, value: Int(request.value), start: request.startDate, end: request.endDate, metadata: metadata)

        default:
            throw HealthWriteError.unsupportedType(request.type)
        }
    }

    private static let supportedUnits: Set<String> = [
        "kg", "g", "mg", "mcg", "lb", "oz", "cm", "m", "km", "in", "ft", "mi", "%", "count", "count/min",
        "mmHg", "mg/dL", "degC", "degF", "kcal", "kJ", "mL", "L", "fl_oz_us", "min", "s", "hr",
    ]

    /// Valid values of the writable category types.
    private static let categoryValues: [String: ClosedRange<Int>] = [
        HKCategoryTypeIdentifier.mindfulSession.rawValue: HKCategoryValue.notApplicable.rawValue...HKCategoryValue.notApplicable.rawValue,
        HKCategoryTypeIdentifier.sleepAnalysis.rawValue: HKCategoryValueSleepAnalysis.inBed.rawValue...HKCategoryValueSleepAnalysis.asleepREM.rawValue,
    ]
}

nonisolated enum HealthWriteError: LocalizedError {
    case unsupportedType(String)
    case unsupportedUnit(String)
    case invalidValue

    var errorDescription: String? {
        switch self {
        case .unsupportedType(let type): "Der Typ \(type) kann nicht in Health geschrieben werden"
        case .unsupportedUnit(let unit): "Die Einheit \"\(unit)\" wird nicht unterstützt"
        case .invalidValue: "Ungültiger Wert oder Zeitraum"
        }
    }
}
