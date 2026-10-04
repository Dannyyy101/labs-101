import Foundation
import HealthKit

/// Arbitrary JSON, used for the free form `payload` of a sample (metadata, workout statistics, ...).
nonisolated enum JSONValue: Codable, Sendable, Equatable {
    case string(String)
    case number(Double)
    case bool(Bool)
    case array([JSONValue])
    case object([String: JSONValue])
    case null

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if container.decodeNil() {
            self = .null
        } else if let value = try? container.decode(Bool.self) {
            self = .bool(value)
        } else if let value = try? container.decode(Double.self) {
            self = .number(value)
        } else if let value = try? container.decode(String.self) {
            self = .string(value)
        } else if let value = try? container.decode([JSONValue].self) {
            self = .array(value)
        } else {
            self = .object(try container.decode([String: JSONValue].self))
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .string(let value): try container.encode(value)
        // JSON has no NaN or infinity
        case .number(let value): value.isFinite ? try container.encode(value) : try container.encodeNil()
        case .bool(let value): try container.encode(value)
        case .array(let value): try container.encode(value)
        case .object(let value): try container.encode(value)
        case .null: try container.encodeNil()
        }
    }

    /// For HealthKit metadata values: strings, numbers, dates and quantities.
    init(any value: Any) {
        switch value {
        case let value as String:
            self = .string(value)
        case let value as NSNumber:
            // Bools are NSNumbers too, so `as Bool` would match every 0 and 1
            self = CFGetTypeID(value) == CFBooleanGetTypeID() ? .bool(value.boolValue) : .number(value.doubleValue)
        case let value as Date:
            self = .string(value.ISO8601Format(.iso8601WithFractionalSeconds))
        case let value as HKQuantity:
            self = .string(value.description)
        case let value as [Any]:
            self = .array(value.map(JSONValue.init(any:)))
        case let value as [String: Any]:
            self = .object(value.mapValues(JSONValue.init(any:)))
        default:
            self = .string(String(describing: value))
        }
    }

    init?(metadata: [String: Any]?) {
        guard let metadata, !metadata.isEmpty else { return nil }
        self = .object(metadata.mapValues(JSONValue.init(any:)))
    }

    /// The value as HealthKit metadata value, nil for arrays, objects and null.
    var metadataValue: Any? {
        switch self {
        case .string(let value): value
        case .number(let value): value
        case .bool(let value): value
        case .array, .object, .null: nil
        }
    }
}

nonisolated extension Date.ISO8601FormatStyle {
    static let iso8601WithFractionalSeconds = Date.ISO8601FormatStyle(includingFractionalSeconds: true)
}
