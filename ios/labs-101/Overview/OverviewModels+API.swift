import Foundation
import LabsAPI

// the DTOs of the backend (generated from api/openapi.json) as the models of the app

nonisolated extension HealthOverview {
    init(_ dto: Components.Schemas.HealthOverviewDto) {
        self.init(date: dto.date, timeZone: dto.timeZone, sleep: dto.sleep.map { Sleep($0.value1) },
                  recovery: Recovery(dto.recovery), strain: Strain(dto.strain), activity: Activity(dto.activity),
                  heartRate: dto.heartRate, workouts: dto.workouts.compactMap(OverviewWorkout.init),
                  vitals: Vitals(vo2Max: dto.vitals.vo2Max, bodyMass: dto.vitals.bodyMass, bodyMassChange: dto.vitals.bodyMassChange),
                  history: dto.history.map(DayScore.init))
    }
}

nonisolated extension Sleep {
    init(_ dto: Components.Schemas.SleepDto) {
        self.init(start: dto.start, end: dto.end, asleep: dto.asleep, inBed: dto.inBed, awake: dto.awake, deep: dto.deep,
                  core: dto.core, rem: dto.rem, need: dto.need, score: Int(dto.score), respiratoryRate: dto.respiratoryRate,
                  oxygenSaturation: dto.oxygenSaturation,
                  // a stage without name is drawn like sleep without a recorded stage
                  stages: dto.stages.map { Stage(stage: SleepStage(rawValue: $0.stage) ?? .asleep, start: $0.start, end: $0.end) })
    }
}

nonisolated extension Recovery {
    init(_ dto: Components.Schemas.RecoveryDto) {
        self.init(score: dto.score.map(Int.init), hrv: dto.hrv, hrvBaseline: dto.hrvBaseline,
                  restingHeartRate: dto.restingHeartRate, restingHeartRateBaseline: dto.restingHeartRateBaseline,
                  respiratoryRate: dto.respiratoryRate, respiratoryRateBaseline: dto.respiratoryRateBaseline,
                  oxygenSaturation: dto.oxygenSaturation, wristTemperatureDeviation: dto.wristTemperatureDeviation)
    }
}

nonisolated extension Strain {
    init(_ dto: Components.Schemas.StrainDto) {
        self.init(score: Int(dto.score), load: dto.load, zoneSeconds: dto.zoneSeconds, maxHeartRate: dto.maxHeartRate,
                  averageHeartRate: dto.averageHeartRate, peakHeartRate: dto.peakHeartRate)
    }
}

nonisolated extension Activity {
    init(_ dto: Components.Schemas.ActivityDto) {
        self.init(steps: dto.steps, distance: dto.distance, activeEnergy: dto.activeEnergy, basalEnergy: dto.basalEnergy,
                  exerciseMinutes: dto.exerciseMinutes, standHours: Int(dto.standHours), flightsClimbed: dto.flightsClimbed,
                  daylightMinutes: dto.daylightMinutes, energyConsumed: dto.energyConsumed)
    }
}

nonisolated extension OverviewWorkout {
    init?(_ dto: Components.Schemas.OverviewWorkoutDto) {
        guard let id = UUID(uuidString: dto.id) else { return nil }
        self.init(id: id, activityType: Int(dto.activityType), startDate: dto.startDate, endDate: dto.endDate,
                  duration: dto.duration, energy: dto.energy, distance: dto.distance, averageHeartRate: dto.averageHeartRate)
    }
}

nonisolated extension DayScore {
    init(_ dto: Components.Schemas.DayScoreDto) {
        self.init(date: dto.date, recovery: dto.recovery.map(Int.init), strain: dto.strain.map(Int.init),
                  sleepScore: dto.sleepScore.map(Int.init), sleep: dto.sleep, hrv: dto.hrv,
                  restingHeartRate: dto.restingHeartRate, steps: dto.steps, activeEnergy: dto.activeEnergy)
    }
}
