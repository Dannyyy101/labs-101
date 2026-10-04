import CoreLocation
import HealthKit

/// Turns HealthKit samples into what the backend stores. Values that have no
/// own column end up in `payload`, so nothing HealthKit knows about a sample is lost.
nonisolated struct HealthSampleEncoder: Sendable {
    let store: HKHealthStore

    func encode(_ sample: HKSample) async -> HealthSampleUpload? {
        var payload = commonPayload(of: sample)
        let kind: HealthSampleKind
        var value: Double?
        var unit: String?

        switch sample {
        case let sample as HKQuantitySample:
            kind = .quantity
            let hkUnit = HealthTypeCatalog.unit(for: sample.quantityType)
            value = sample.quantity.doubleValue(for: hkUnit)
            unit = hkUnit.unitString
            // series samples (e.g. heart rate during a workout) hold the average of `count` values
            if sample.count > 1 {
                payload["count"] = .number(Double(sample.count))
            }

        case let sample as HKCategorySample:
            kind = .category
            value = Double(sample.value)

        case let workout as HKWorkout:
            kind = .workout
            value = workout.duration
            unit = "s"
            payload.merge(await workoutPayload(workout)) { $1 }

        case let correlation as HKCorrelation:
            kind = .correlation
            payload["objects"] = .array(correlation.objects.sorted { $0.uuid.uuidString < $1.uuid.uuidString }.map {
                .object(["uuid": .string($0.uuid.uuidString), "type": .string($0.sampleType.identifier)])
            })

        case let ecg as HKElectrocardiogram:
            kind = .ecg
            value = ecg.averageHeartRate?.doubleValue(for: .count().unitDivided(by: .minute()))
            unit = value == nil ? nil : "count/min"
            payload.merge(await ecgPayload(ecg)) { $1 }

        case let audiogram as HKAudiogramSample:
            kind = .audiogram
            payload["sensitivityPoints"] = .array(audiogram.sensitivityPoints.map(Self.sensitivityPoint))

        case let stateOfMind as HKStateOfMind:
            kind = .stateOfMind
            value = stateOfMind.valence
            payload["kind"] = .number(Double(stateOfMind.kind.rawValue))
            payload["valenceClassification"] = .number(Double(stateOfMind.valenceClassification.rawValue))
            payload["labels"] = .array(stateOfMind.labels.map { .number(Double($0.rawValue)) })
            payload["associations"] = .array(stateOfMind.associations.map { .number(Double($0.rawValue)) })

        case let assessment as HKScoredAssessment:
            kind = .scoredAssessment
            value = Double(assessment.score)
            if let gad7 = assessment as? HKGAD7Assessment {
                payload["risk"] = .number(Double(gad7.risk.rawValue))
                payload["answers"] = .array(gad7.answers.map { .number(Double($0.rawValue)) })
            } else if let phq9 = assessment as? HKPHQ9Assessment {
                payload["risk"] = .number(Double(phq9.risk.rawValue))
                payload["answers"] = .array(phq9.answers.map { .number(Double($0.rawValue)) })
            }

        default:
            return nil
        }

        return HealthSampleUpload(
            uuid: sample.uuid,
            kind: kind,
            type: sample.sampleType.identifier,
            startDate: sample.startDate,
            endDate: sample.endDate,
            value: value.flatMap { $0.isFinite ? $0 : nil },
            unit: unit,
            sourceName: sample.sourceRevision.source.name,
            sourceBundleId: sample.sourceRevision.source.bundleIdentifier,
            payload: payload.isEmpty ? nil : .object(payload))
    }

    // MARK: Common

    private func commonPayload(of sample: HKSample) -> [String: JSONValue] {
        var payload: [String: JSONValue] = [:]
        if let metadata = JSONValue(metadata: sample.metadata) {
            payload["metadata"] = metadata
        }

        let revision = sample.sourceRevision
        var source: [String: JSONValue] = [:]
        source["version"] = revision.version.map(JSONValue.string)
        source["productType"] = revision.productType.map(JSONValue.string)
        let os = revision.operatingSystemVersion
        source["operatingSystemVersion"] = .string("\(os.majorVersion).\(os.minorVersion).\(os.patchVersion)")
        payload["source"] = .object(source)

        if let device = sample.device {
            var values: [String: JSONValue] = [:]
            values["name"] = device.name.map(JSONValue.string)
            values["manufacturer"] = device.manufacturer.map(JSONValue.string)
            values["model"] = device.model.map(JSONValue.string)
            values["hardwareVersion"] = device.hardwareVersion.map(JSONValue.string)
            values["softwareVersion"] = device.softwareVersion.map(JSONValue.string)
            values["firmwareVersion"] = device.firmwareVersion.map(JSONValue.string)
            values["localIdentifier"] = device.localIdentifier.map(JSONValue.string)
            values["udiDeviceIdentifier"] = device.udiDeviceIdentifier.map(JSONValue.string)
            payload["device"] = .object(values)
        }
        return payload
    }

    // MARK: Workouts

    private func workoutPayload(_ workout: HKWorkout) async -> [String: JSONValue] {
        var payload: [String: JSONValue] = [
            "activityType": .number(Double(workout.workoutActivityType.rawValue)),
            "statistics": Self.statistics(workout.allStatistics),
        ]
        if let events = workout.workoutEvents, !events.isEmpty {
            payload["events"] = .array(events.map { event in
                var values: [String: JSONValue] = [
                    "type": .number(Double(event.type.rawValue)),
                    "startDate": .string(event.dateInterval.start.ISO8601Format(.iso8601WithFractionalSeconds)),
                    "endDate": .string(event.dateInterval.end.ISO8601Format(.iso8601WithFractionalSeconds)),
                ]
                values["metadata"] = JSONValue(metadata: event.metadata)
                return .object(values)
            })
        }
        if !workout.workoutActivities.isEmpty {
            payload["activities"] = .array(workout.workoutActivities.map { activity in
                var values: [String: JSONValue] = [
                    "uuid": .string(activity.uuid.uuidString),
                    "activityType": .number(Double(activity.workoutConfiguration.activityType.rawValue)),
                    "locationType": .number(Double(activity.workoutConfiguration.locationType.rawValue)),
                    "swimmingLocationType": .number(Double(activity.workoutConfiguration.swimmingLocationType.rawValue)),
                    "startDate": .string(activity.startDate.ISO8601Format(.iso8601WithFractionalSeconds)),
                    "duration": .number(activity.duration),
                    "statistics": Self.statistics(activity.allStatistics),
                ]
                values["endDate"] = activity.endDate.map { .string($0.ISO8601Format(.iso8601WithFractionalSeconds)) }
                if let lapLength = activity.workoutConfiguration.lapLength {
                    values["lapLength"] = .number(lapLength.doubleValue(for: .meter()))
                }
                values["metadata"] = JSONValue(metadata: activity.metadata)
                return .object(values)
            })
        }
        // without the route the workout itself is still worth syncing
        if let routes = try? await routes(of: workout), !routes.isEmpty {
            payload["routes"] = .array(routes)
        }
        return payload
    }

    private static func statistics(_ statistics: [HKQuantityType: HKStatistics]) -> JSONValue {
        .object(Dictionary(uniqueKeysWithValues: statistics.map { type, statistics in
            let unit = HealthTypeCatalog.unit(for: type)
            var values: [String: JSONValue] = ["unit": .string(unit.unitString)]
            values["sum"] = statistics.sumQuantity().map { .number($0.doubleValue(for: unit)) }
            values["average"] = statistics.averageQuantity().map { .number($0.doubleValue(for: unit)) }
            values["minimum"] = statistics.minimumQuantity().map { .number($0.doubleValue(for: unit)) }
            values["maximum"] = statistics.maximumQuantity().map { .number($0.doubleValue(for: unit)) }
            return (type.identifier, .object(values))
        }))
    }

    /// GPS track of the workout, as compact arrays to keep the upload small:
    /// [seconds since workout start, latitude, longitude, altitude, speed, course, horizontal accuracy, vertical accuracy]
    private func routes(of workout: HKWorkout) async throws -> [JSONValue] {
        let routeQuery = HKSampleQueryDescriptor(
            predicates: [.workoutRoute(HKQuery.predicateForObjects(from: workout))],
            sortDescriptors: [SortDescriptor(\.startDate)])
        var routes: [JSONValue] = []
        for route in try await routeQuery.result(for: store) {
            var points: [JSONValue] = []
            for try await location in HKWorkoutRouteQueryDescriptor(route).results(for: store) {
                points.append(.array([
                    Self.rounded(location.timestamp.timeIntervalSince(workout.startDate), 1),
                    Self.rounded(location.coordinate.latitude, 6),
                    Self.rounded(location.coordinate.longitude, 6),
                    Self.rounded(location.altitude, 1),
                    Self.rounded(location.speed, 2),
                    Self.rounded(location.course, 1),
                    Self.rounded(location.horizontalAccuracy, 1),
                    Self.rounded(location.verticalAccuracy, 1),
                ]))
            }
            routes.append(.object(["uuid": .string(route.uuid.uuidString), "points": .array(points)]))
        }
        return routes
    }

    private static func rounded(_ value: Double, _ decimals: Int) -> JSONValue {
        let factor = pow(10, Double(decimals))
        return .number((value * factor).rounded() / factor)
    }

    // MARK: ECG

    private func ecgPayload(_ ecg: HKElectrocardiogram) async -> [String: JSONValue] {
        var payload: [String: JSONValue] = [
            "classification": .number(Double(ecg.classification.rawValue)),
            "symptomsStatus": .number(Double(ecg.symptomsStatus.rawValue)),
            "numberOfVoltageMeasurements": .number(Double(ecg.numberOfVoltageMeasurements)),
        ]
        payload["samplingFrequency"] = ecg.samplingFrequency.map { .number($0.doubleValue(for: .hertz())) }

        // lead I like voltages in microvolts, `samplingFrequency` apart
        var voltages: [JSONValue] = []
        voltages.reserveCapacity(ecg.numberOfVoltageMeasurements)
        do {
            for try await measurement in HKElectrocardiogramQueryDescriptor(ecg).results(for: store) {
                let microvolts = measurement.quantity(for: .appleWatchSimilarToLeadI)?.doubleValue(for: .voltUnit(with: .micro))
                voltages.append(microvolts.map { Self.rounded($0, 3) } ?? .null)
            }
            payload["voltages"] = .array(voltages)
        } catch {
            // the summary of the ECG is still worth syncing
        }
        return payload
    }

    // MARK: Audiogram

    private static func sensitivityPoint(_ point: HKAudiogramSensitivityPoint) -> JSONValue {
        .object([
            "frequency": .number(point.frequency.doubleValue(for: .hertz())),
            "tests": .array(point.tests.map { test in
                var values: [String: JSONValue] = [
                    "sensitivity": .number(test.sensitivity.doubleValue(for: .decibelHearingLevel())),
                    "type": .number(Double(test.type.rawValue)),
                    "masked": .bool(test.masked),
                    "side": .number(Double(test.side.rawValue)),
                ]
                if let range = test.clampingRange {
                    values["clampingLowerBound"] = range.lowerBound.map { .number($0.doubleValue(for: .decibelHearingLevel())) }
                    values["clampingUpperBound"] = range.upperBound.map { .number($0.doubleValue(for: .decibelHearingLevel())) }
                }
                return .object(values)
            }),
        ])
    }
}
