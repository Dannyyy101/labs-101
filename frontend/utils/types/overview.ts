// the home page: sleep, recovery and strain of a day, see HealthOverviewController in the backend
// times in seconds, energy in kcal, distances in meters, scores 0–100

export type SleepStage = "AWAKE" | "CORE" | "DEEP" | "REM" | "ASLEEP"

export interface Sleep {
    start: string
    end: string
    // all sleep ending on the day, naps included
    asleep: number
    inBed: number
    awake: number
    deep: number
    core: number
    rem: number
    need: number
    score: number
    respiratoryRate: number | null
    // 0–1
    oxygenSaturation: number | null
    stages: { stage: SleepStage, start: string, end: string }[]
}

export interface Recovery {
    // null until there are a few nights to compare with
    score: number | null
    hrv: number | null
    hrvBaseline: number | null
    restingHeartRate: number | null
    restingHeartRateBaseline: number | null
    respiratoryRate: number | null
    respiratoryRateBaseline: number | null
    oxygenSaturation: number | null
    // °C compared with the baseline
    wristTemperatureDeviation: number | null
}

export interface Strain {
    score: number
    load: number
    // Z1–Z5 at 50, 60, 70, 80 and 90 % of maxHeartRate
    zoneSeconds: number[]
    maxHeartRate: number
    averageHeartRate: number | null
    peakHeartRate: number | null
}

export interface Activity {
    steps: number
    distance: number
    activeEnergy: number
    basalEnergy: number
    exerciseMinutes: number
    standHours: number
    flightsClimbed: number
    daylightMinutes: number
    energyConsumed: number | null
}

export interface OverviewWorkout {
    id: string
    // HKWorkoutActivityType
    activityType: number
    startDate: string
    endDate: string
    duration: number
    energy: number | null
    distance: number | null
    averageHeartRate: number | null
}

export interface DayScore {
    date: string
    recovery: number | null
    strain: number | null
    sleepScore: number | null
    sleep: number | null
    hrv: number | null
    restingHeartRate: number | null
    steps: number
    activeEnergy: number
}

export interface HealthOverview {
    date: string
    timeZone: string
    sleep: Sleep | null
    recovery: Recovery
    strain: Strain
    activity: Activity
    // [epoch millis, bpm] every 5 minutes
    heartRate: [number, number][]
    workouts: OverviewWorkout[]
    vitals: { vo2Max: number | null, bodyMass: number | null, bodyMassChange: number | null }
    // the 14 days up to date, oldest first
    history: DayScore[]
}
