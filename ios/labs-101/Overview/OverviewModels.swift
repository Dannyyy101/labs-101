import Foundation

// Sleep, recovery and strain of a day, see HealthOverviewController in the backend.
// Times in seconds, energy in kcal, distances in meters, scores 0–100.

nonisolated struct HealthOverview: Decodable, Sendable {
    /// "YYYY-MM-DD"
    let date: String
    let timeZone: String
    let sleep: Sleep?
    let recovery: Recovery
    let strain: Strain
    let activity: Activity
    /// [epoch millis, bpm] every 5 minutes
    let heartRate: [[Double]]
    let workouts: [OverviewWorkout]
    let vitals: Vitals
    /// the 14 days up to date, oldest first
    let history: [DayScore]

    var hasData: Bool {
        history.contains { $0.sleep != nil || $0.steps > 0 || $0.strain != nil }
    }

    var heartRatePoints: [HeartRatePoint] {
        heartRate.compactMap { pair in
            guard pair.count == 2 else { return nil }
            return HeartRatePoint(date: Date(timeIntervalSince1970: pair[0] / 1000), bpm: pair[1])
        }
    }
}

nonisolated struct HeartRatePoint: Hashable, Sendable {
    let date: Date
    let bpm: Double
}

nonisolated struct Sleep: Decodable, Sendable {
    nonisolated struct Stage: Decodable, Hashable, Sendable {
        let stage: SleepStage
        let start: Date
        let end: Date
    }

    let start: Date
    let end: Date
    /// all sleep ending on the day, naps included
    let asleep: Double
    let inBed: Double
    let awake: Double
    let deep: Double
    let core: Double
    let rem: Double
    let need: Double
    let score: Int
    let respiratoryRate: Double?
    /// 0–1
    let oxygenSaturation: Double?
    let stages: [Stage]
}

nonisolated enum SleepStage: String, Decodable, Sendable {
    case awake = "AWAKE", rem = "REM", core = "CORE", deep = "DEEP"
    /// no stage recorded, drawn as core sleep
    case asleep = "ASLEEP"
}

nonisolated struct Recovery: Decodable, Sendable {
    /// nil until there are a few nights to compare with
    let score: Int?
    let hrv: Double?
    let hrvBaseline: Double?
    let restingHeartRate: Double?
    let restingHeartRateBaseline: Double?
    let respiratoryRate: Double?
    let respiratoryRateBaseline: Double?
    let oxygenSaturation: Double?
    /// °C compared with the baseline
    let wristTemperatureDeviation: Double?
}

nonisolated struct Strain: Decodable, Sendable {
    let score: Int
    let load: Double
    /// Z1–Z5 at 50, 60, 70, 80 and 90 % of maxHeartRate
    let zoneSeconds: [Double]
    let maxHeartRate: Double
    let averageHeartRate: Double?
    let peakHeartRate: Double?
}

nonisolated struct Activity: Decodable, Sendable {
    let steps: Double
    let distance: Double
    let activeEnergy: Double
    let basalEnergy: Double
    let exerciseMinutes: Double
    let standHours: Int
    let flightsClimbed: Double
    let daylightMinutes: Double
    let energyConsumed: Double?
}

nonisolated struct OverviewWorkout: Decodable, Identifiable, Sendable {
    let id: UUID
    /// HKWorkoutActivityType
    let activityType: Int
    let startDate: Date
    let endDate: Date
    let duration: Double
    let energy: Double?
    let distance: Double?
    let averageHeartRate: Double?
}

nonisolated struct Vitals: Decodable, Sendable {
    let vo2Max: Double?
    let bodyMass: Double?
    /// kg compared with 30 days before
    let bodyMassChange: Double?
}

nonisolated struct DayScore: Decodable, Identifiable, Sendable {
    /// "YYYY-MM-DD"
    let date: String
    let recovery: Int?
    let strain: Int?
    let sleepScore: Int?
    let sleep: Double?
    let hrv: Double?
    let restingHeartRate: Double?
    let steps: Double
    let activeEnergy: Double

    var id: String { date }
}
